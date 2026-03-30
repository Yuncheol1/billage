import os
import json
import time
import torch
import psutil
import chromadb
from sentence_transformers import SentenceTransformer
from tabulate import tabulate

from config import OUTPUT_DIR, CHROMA_DB_DIR, BATCH_SIZE

# --- 테스트할 모델 리스트 ---
CANDIDATE_MODELS = [
    "BAAI/bge-m3",
    "jhgan/ko-sroberta-multitask",
    "intfloat/multilingual-e5-large"
]

def load_gold_dataset():
    file_path = os.path.join(OUTPUT_DIR, "gold_dataset_100.json")
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"테스트 데이터셋 파일이 없습니다: {file_path}. 05_generate_test_dataset.py를 먼저 실행하세요.")
    
    with open(file_path, 'r', encoding='utf-8') as f:
        return json.load(f)

def load_parsed_articles():
    file_path = os.path.join(OUTPUT_DIR, "parsed_articles.json")
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"파싱된 파일이 없습니다: {file_path}")
    
    with open(file_path, 'r', encoding='utf-8') as f:
        return json.load(f)

def format_memory(bytes_val):
    return f"{bytes_val / (1024**3):.2f} GB"

def run_benchmark():
    print("="*60)
    print("[START] 임베딩 모델 벤치마킹 테스트 시작")
    print("="*60)
    
    articles = load_parsed_articles()
    gold_dataset = load_gold_dataset()
    total_articles = len(articles)
    print(f"총 {total_articles}개의 문서(조항) 로드 완료.")
    print(f"총 {len(gold_dataset)}개의 테스트 쿼리 로드 완료.")

    client = chromadb.PersistentClient(path=CHROMA_DB_DIR)

    results = []
    
    for model_name in CANDIDATE_MODELS:
        print("-"*60)
        print(f"[EVAL] 모델 평가 시작: {model_name}")
        print("-"*60)
        
        # 모델명 포맷 (슬래시 제거)
        safe_model_name = model_name.replace("/", "_").replace("-", "_")
        collection_name = f"legal_docs_{safe_model_name}"
        
        # GPU 캐시 정리 (정확한 측정을 위해)
        if torch.cuda.is_available():
            torch.cuda.empty_cache()
            torch.cuda.reset_peak_memory_stats()
        
        # 1. 모델 로드
        load_start = time.time()
        print(f"1) 모델 로딩 중... (torch_dtype=float16 설정 적용)")
        # 메모리 효율을 위해 float16 로드 강제
        model_kwargs = {"torch_dtype": torch.float16} if torch.cuda.is_available() else {}
        model = SentenceTransformer(model_name, model_kwargs=model_kwargs)
        
        # 초기 VRAM 할당량 기록
        if torch.cuda.is_available():
            vram_init = torch.cuda.memory_allocated()
        else:
            vram_init = psutil.Process(os.getpid()).memory_info().rss
            
        print(f"   - 모델 로딩 소요 시간: {time.time() - load_start:.2f}초")
        print(f"   - 초기 메모리 사용량: {format_memory(vram_init)}")
        
        # 2. 크로마 DB 설정
        print(f"2) ChromaDB 컬렉션 생성 ({collection_name})")
        try:
            client.delete_collection(name=collection_name)
        except Exception:
            pass
        collection = client.create_collection(
            name=collection_name, 
            metadata={"hnsw:space": "cosine"}
        )

        # 3. 인덱싱 (배치 임베딩)
        print(f"3) 임베딩 및 인덱싱 시작 (Batch Size: {BATCH_SIZE})")
        encode_start = time.time()
        
        texts = [item["content"] for item in articles]
        metadatas = [{"source": item["source"], "article_id": item["article_id"]} for item in articles]
        ids = [f"{safe_model_name}_doc_{i}" for i in range(total_articles)]
        
        # 배치 인코딩
        all_embeddings = []
        for i in range(0, total_articles, BATCH_SIZE):
            batch_texts = texts[i:i + BATCH_SIZE]
            embeddings = model.encode(batch_texts, normalize_embeddings=True)
            if hasattr(embeddings, 'tolist'):
                all_embeddings.extend(embeddings.tolist())
            else:
                all_embeddings.extend([e.tolist() for e in embeddings])
                
        encode_time = time.time() - encode_start
        throughput = total_articles / encode_time
        
        print(f"   - 총 인코딩 소요 시간: {encode_time:.2f}초 ({throughput:.1f} 문장/초)")
        
        # Peak VRAM 기록
        if torch.cuda.is_available():
            vram_peak = torch.cuda.max_memory_allocated()
            print(f"   - 인코딩 중 Peak VRAM: {format_memory(vram_peak)}")
        else:
            vram_peak = psutil.Process(os.getpid()).memory_info().rss
            print(f"   - 인코딩 중 RAM Usage: {format_memory(vram_peak)}")
            
        # DB 저장
        INSERT_BATCH_SIZE = 100
        for i in range(0, len(ids), INSERT_BATCH_SIZE):
            collection.add(
                documents=texts[i:i + INSERT_BATCH_SIZE],
                embeddings=all_embeddings[i:i + INSERT_BATCH_SIZE],
                metadatas=metadatas[i:i + INSERT_BATCH_SIZE],
                ids=ids[i:i + INSERT_BATCH_SIZE]
            )
            
        # 4. 평가 (Gold Dataset 쿼리)
        print(f"4) 쿼리 테스트 시작 (Total Queries: {len(gold_dataset)})")
        
        hits = 0
        total_mrr = 0
        total_latency = 0
        
        for data in gold_dataset:
            query = data["query"]
            target_article = data["target_article_id"]
            
            q_start = time.time()
            query_embedding = model.encode([query], normalize_embeddings=True)
            search_res = collection.query(
                query_embeddings=query_embedding.tolist() if hasattr(query_embedding, 'tolist') else [e.tolist() for e in query_embedding],
                n_results=3,
                include=["metadatas"]
            )
            q_latency = time.time() - q_start
            total_latency += q_latency
            
            retrieved_articles = [meta["article_id"] for meta in search_res['metadatas'][0]]
            
            rank = -1
            for idx, article in enumerate(retrieved_articles):
                if target_article in article or article in target_article:
                    rank = idx + 1
                    break
                    
            if rank != -1:
                hits += 1           
                total_mrr += (1.0 / rank)
                
        hit_rate = (hits / len(gold_dataset)) * 100
        avg_mrr = total_mrr / len(gold_dataset)
        avg_latency = (total_latency / len(gold_dataset)) * 1000 # ms 변환

        print(f"   - Hit Rate@3: {hit_rate:.1f}%")
        print(f"   - MRR: {avg_mrr:.3f}")
        print(f"   - Average Query Latency: {avg_latency:.1f}ms")
        
        # 5. 결과 저장
        results.append({
            "Model": model_name,
            "Hit Rate@3": f"{hit_rate:.1f}%",
            "MRR": f"{avg_mrr:.3f}",
            "Latency (ms)": f"{avg_latency:.1f}ms",
            "Throughput": f"{throughput:.1f} doc/s",
            "Peak VRAM": format_memory(vram_peak)
        })
        
        # 참조 제거 후 가비지 컬렉터 강제 실행될 수 있게 삭제
        del model
        if torch.cuda.is_available():
            torch.cuda.empty_cache()
            
    # --- 종합 보고서 (마크다운) 생성 ---
    generate_markdown_report(results)
    
    # --- 결과 출력 ---
    print("\n\n" + "="*80)
    print("[RESULT] 임베딩 모델 벤치마킹 테스트 최종 결과")
    print("="*80)
    
    headers = "keys"
    table = tabulate(results, headers="keys", tablefmt="pretty")
    print(table)
    print("\n결과 보고서가 'benchmark_report.md'로 저장되었습니다.")


def generate_markdown_report(results):
    report_content = "# 임베딩 모델 벤치마킹 수행 보고서\n\n"
    report_content += "본 보고서는 한국어 법률/금융 도메인의 테스트 셋팅 환경에서 수행된 3가지 후보 임베딩 모델의 벤치마킹 결과입니다.\n\n"
    
    report_content += "## 1. 종합 성능 비교 표\n\n"
    
    # 마크다운 표 생성
    headers = list(results[0].keys())
    header_row = "| " + " | ".join(headers) + " |"
    divider_row = "| " + " | ".join(["---"] * len(headers)) + " |"
    
    report_content += header_row + "\n" + divider_row + "\n"
    
    for row in results:
        row_str = "| " + " | ".join(str(row[h]) for h in headers) + " |"
        report_content += row_str + "\n"
        
    report_content += "\n## 2. 측정 지표 설명\n"
    report_content += "- **Hit Rate@3**: 상위 3개 검색 결과 중 정답이 포함된 비율\n"
    report_content += "- **MRR(Mean Reciprocal Rank)**: 정답의 평균 순위 (1위에 가까울수록 1.0)\n"
    report_content += "- **Latency (ms)**: 모델의 `query` 테스트 1회당 소요되는 평균 시간(초 단위 → ms)\n"
    report_content += "- **Throughput**: 1초당 임베딩 변환 및 인덱싱한 문서(조항) 수\n"
    report_content += "- **Peak VRAM**: 인코딩 과정에서 GPU 메모리가 최대로 할당된 수량\n\n"
    
    report_content += "## 3. 평가 요약\n"
    report_content += "> 결과에 따라 서비스 특성(비용 최우선 vs 성능 최우선)에 맞춰 1가지 모델을 최종 시스템에 반영할 수 있습니다.\n"
    
    output_path = os.path.join(os.path.dirname(__file__), "benchmark_report.md")
    with open(output_path, "w", encoding='utf-8') as f:
        f.write(report_content)


if __name__ == "__main__":
    run_benchmark()

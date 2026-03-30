import os
import time
import psutil
import torch
from sentence_transformers import SentenceTransformer
import chromadb
from tabulate import tabulate

from config import CHROMA_DB_DIR, COLLECTION_NAME, EMBEDDING_MODEL_NAME

# --- 1. Gold Dataset (테스트용 질문 5개) ---
GOLD_DATASET = [
    {
        "query": "법정 최고 이자율은 얼마인가요?",
        "target_source": "이자제한법(법률)(제20714호)(20250722).pdf",
        "target_article_id": "제2조(이자의 최고한도)"
    },
    {
        "query": "미성년자가 물건을 사려면 부모님 동의가 필요한가요?",
        "target_source": "민법(법률)(제20432호)(20260101).pdf",
        "target_article_id": "제5조(미성년자의 능력)"
    },
    {
        "query": "비영리법인을 설립하려면 어떻게 해야 하나요?",
        "target_source": "민법(법률)(제20432호)(20260101).pdf",
        "target_article_id": "제32조(비영리법인의 설립과 허가)"
    },
    {
        "query": "실종 선고는 몇 년 동안 연락이 안 되면 할 수 있나요?",
        "target_source": "민법(법률)(제20432호)(20260101).pdf",
        "target_article_id": "제27조(실종의 선고)"
    },
    {
        "query": "선이자를 미리 떼고 돈을 빌려줬을 때 어떻게 계산하나요?",
        "target_source": "이자제한법(법률)(제20714호)(20250722).pdf",
        "target_article_id": "제3조(이자의 공제)"
    }
]

def get_vram_usage():
    """현재 GPU(VRAM) 또는 시스템 RAM 사용량을 반환합니다."""
    if torch.cuda.is_available():
        allocated = torch.cuda.memory_allocated() / (1024 ** 3)  # GB 단위
        reserved = torch.cuda.memory_reserved() / (1024 ** 3)
        return f"VRAM: {allocated:.2f}GB (Reserved: {reserved:.2f}GB)"
    else:
        # GPU가 없으면 시스템 RAM 사용량 반환
        process = psutil.Process(os.getpid())
        mem_info = process.memory_info()
        return f"RAM: {mem_info.rss / (1024 ** 3):.2f}GB"

def run_evaluation():
    print("시스템 및 모델 로딩 중...")
    
    # 모델 및 DB 로드
    model = SentenceTransformer(EMBEDDING_MODEL_NAME)
    client = chromadb.PersistentClient(path=CHROMA_DB_DIR)
    collection = client.get_collection(name=COLLECTION_NAME)

    print(f"\n[초기 자원 상태] {get_vram_usage()}\n")
    print(f"총 {len(GOLD_DATASET)}개의 질문에 대한 평가를 시작합니다...\n")

    results_table = []
    total_latency = 0
    hits = 0
    total_mrr = 0

    for idx, data in enumerate(GOLD_DATASET, 1):
        query = data["query"]
        target_article = data["target_article_id"]
        
        # 1. 쿼리 시간 측정 시작
        start_time = time.time()
        
        # 2. 임베딩 및 검색
        query_embedding = model.encode([query], normalize_embeddings=True)
        results = collection.query(
            query_embeddings=query_embedding.tolist() if hasattr(query_embedding, 'tolist') else [e.tolist() for e in query_embedding],
            n_results=3,  # Top-3 검색
            include=["metadatas"]
        )
        
        # 3. 쿼리 시간 측정 종료
        latency = time.time() - start_time
        total_latency += latency
        
        # 4. 결과 분석 (Hit Rate & MRR)
        retrieved_articles = [meta["article_id"] for meta in results['metadatas'][0]]
        
        rank = -1
        for i, article in enumerate(retrieved_articles):
            if target_article in article or article in target_article:
                rank = i + 1
                break
                
        if rank != -1:
            hits += 1           # 3위 안에 들면 Hit!
            mrr = 1.0 / rank    # 1위: 1.0, 2위: 0.5, 3위: 0.33
            status = f"✅ Hit ({rank}위)"
        else:
            mrr = 0.0
            status = "❌ Miss"
            
        total_mrr += mrr
        
        # 5. 리포트용 데이터 적재
        results_table.append([
            idx, 
            query[:20] + "...", 
            target_article, 
            status, 
            f"{latency:.3f}s"
        ])

    # --- 평가 지표 계산 ---
    avg_latency = total_latency / len(GOLD_DATASET)
    hit_rate = (hits / len(GOLD_DATASET)) * 100
    mean_rr = total_mrr / len(GOLD_DATASET)
    final_vram = get_vram_usage()
    
    # --- 결과 출력 ---
    print("\n" + "="*60)
    print("📊 AI 법률 비서 성능 평가 ('도서관 사서' 테스트)")
    print("="*60)
    
    headers = ["No", "질문 (Query)", "정답 조항 (Ground Truth)", "판정 (Rank)", "응답 속도"]
    print(tabulate(results_table, headers=headers, tablefmt="pretty"))
    
    print("\n🔍 **최종 평가 요약 (Summary)** 🔍")
    print(f"- 정답 찾기 (Hit Rate@3) : {hit_rate:.1f}% (목표 90% 이상)")
    print(f"- 평균 검색 순위 (MRR)   : {mean_rr:.3f}  (1.0에 가까울수록 완벽)")
    print(f"- 평균 응답 속도         : {avg_latency:.3f}초 (목표 5초 이내)")
    print(f"- GPU/RAM 사용량         : {final_vram}")
    
    if hit_rate >= 90 and avg_latency <= 5:
        print("\n🎉 종합 판정: 성공 (SUCCESS) - 파이프라인이 훌륭하게 최적화되었습니다!")
    else:
        print("\n⚠️ 종합 판정: 개선 필요 (NEEDS IMPROVEMENT) - 임베딩 튜닝 또는 DB 재편이 필요할 수 있습니다.")

if __name__ == "__main__":
    run_evaluation()

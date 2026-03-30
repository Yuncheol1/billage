import os
# [MANDATORY] 공유 서버 0번 GPU 할당 강제 지정
os.environ["CUDA_DEVICE_ORDER"] = "PCI_BUS_ID"
os.environ["CUDA_VISIBLE_DEVICES"] = "0"

import json
import time
import torch
import gc
from sentence_transformers import SentenceTransformer
import chromadb

from config import OUTPUT_DIR, CHROMA_DB_DIR, COLLECTION_NAME, EMBEDDING_MODEL_NAME, BATCH_SIZE

def load_parsed_articles():
    file_path = os.path.join(OUTPUT_DIR, "parsed_articles.json")
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"파싱된 파일이 없습니다: {file_path}. 먼저 01_extract_and_parse.py를 실행하세요.")
    
    with open(file_path, 'r', encoding='utf-8') as f:
        return json.load(f)

# [CHECKPOINT] 진행 상황 저장용 파일
CHECKPOINT_FILE = os.path.join(OUTPUT_DIR, "embedding_checkpoint.json")

def get_last_processed_index():
    if os.path.exists(CHECKPOINT_FILE):
        with open(CHECKPOINT_FILE, 'r') as f:
            data = json.load(f)
            return data.get("last_index", 0)
    return 0

def save_checkpoint(index):
    with open(CHECKPOINT_FILE, 'w') as f:
        json.dump({"last_index": index}, f)

def embed_and_store():
    print("1. 파싱된 데이터 로딩...")
    articles = load_parsed_articles()
    total_articles = len(articles)
    print(f"   => 총 {total_articles}개의 문서(조항) 로드 완료.")

    print(f"\n2. 임베딩 모델 로드 중 ({EMBEDDING_MODEL_NAME})... 잠시만 기다려주세요.")
    
    # [STRICT RULE] VRAM 40GB Limit Setting
    # 전체 144GB의 약 27% 수준을 할당하면 40GB 이내로 안전하게 유지됨
    if torch.cuda.is_available():
        memory_fraction = 0.27
        torch.cuda.set_per_process_memory_fraction(memory_fraction, 0)
        print(f"   => GPU 0번 VRAM 사용량을 {memory_fraction*100:.1f}%(약 40GB 제한)로 락 설정 완료.")
        
    # 속도를 위해 float16 로드 강제
    model_kwargs = {"torch_dtype": torch.float16} if torch.cuda.is_available() else {}
    model = SentenceTransformer(EMBEDDING_MODEL_NAME, model_kwargs=model_kwargs)
    print("   => 모델 로드 완료 (dtype: float16 적용).")

    print(f"\n3. 로컬 Chroma DB 연결 (경로: {CHROMA_DB_DIR})")
    client = chromadb.PersistentClient(path=CHROMA_DB_DIR)
    
    last_index = get_last_processed_index()
    
    # 기존 컬렉션이 있다면 가져오거나 없으면 생성
    if last_index == 0:
        try:
            client.delete_collection(name=COLLECTION_NAME)
            print("   => 기존 컬렉션을 삭제했습니다. (새로 시작)")
        except Exception:
            pass
            
        collection = client.create_collection(
            name=COLLECTION_NAME,
            metadata={"hnsw:space": "cosine"} # 코사인 유사도 사용
        )
    else:
        collection = client.get_or_create_collection(
            name=COLLECTION_NAME,
            metadata={"hnsw:space": "cosine"}
        )
        print(f"   => [RESUME] 체크포인트 발견! 인덱스 {last_index}번부터 이어서 시작(Resume)합니다.")

    print(f"\n4. 임베딩 배치 생성 및 DB 적재 시작 (Batch Size: {BATCH_SIZE})")
    
    start_time = time.time()
    
    texts = []
    metadatas = []
    ids = []
    all_embeddings = []
    
    # [RESUME] 지난 번 저장된 위치부터 스킵하고 처리
    for i in range(last_index, total_articles, BATCH_SIZE):
        batch_articles = articles[i:i + BATCH_SIZE]
        batch_texts = [item["content"] for item in batch_articles]
        
        # 임베딩 생성
        # BGE-M3는 내부적으로 normalize_embeddings 파라미터를 지원함
        embeddings = model.encode(batch_texts, normalize_embeddings=True)
        if hasattr(embeddings, 'tolist'):
            all_embeddings.extend(embeddings.tolist())
        else:
            all_embeddings.extend([e.tolist() for e in embeddings])
        
        # 메타데이터 및 ID 할당
        for j, item in enumerate(batch_articles):
            doc_idx = i + j
            doc_id = f"doc_{doc_idx}"
            
            # DB 추가용 리스트에 보관
            texts.append(batch_texts[j])
            metadatas.append({
                "source": item["source"],
                "article_id": item["article_id"]
            })
            ids.append(doc_id)
            
        print(f"   - Progress: {min(i + BATCH_SIZE, total_articles)} / {total_articles} 저장 준비 완료")
        
        # 중간 저장 (Insert to ChromaDB in chunks to prevent memory bloat)
        INSERT_BATCH_SIZE = 100
        if len(ids) >= INSERT_BATCH_SIZE or (i + BATCH_SIZE) >= total_articles:
            print("\n   => 생성된 임베딩 묶음을 DB에 적재 중...")
            collection.add(
                documents=texts,
                embeddings=all_embeddings,
                metadatas=metadatas,
                ids=ids
            )
            # 체크포인트 기록
            save_checkpoint(min(i + BATCH_SIZE, total_articles))
            
            # 리스트 초기화 (VRAM/RAM 동시 확보)
            texts.clear()
            metadatas.clear()
            ids.clear()
            all_embeddings.clear()
            
        # [MEMORY PROTECTION] 가비지 컬렉션 및 캐시 해제 (세션 공유 서버 필수 요구사항)
        del embeddings
        del batch_texts
        del batch_articles
        gc.collect()
        if torch.cuda.is_available():
            torch.cuda.empty_cache()
    
    elapsed = time.time() - start_time
    print(f"\n[Success] 적재 완료! 총 소요 시간: {elapsed:.2f}초")
    
    # 완료 후 체크포인트 초기화
    if os.path.exists(CHECKPOINT_FILE):
        os.remove(CHECKPOINT_FILE)

if __name__ == "__main__":
    embed_and_store()

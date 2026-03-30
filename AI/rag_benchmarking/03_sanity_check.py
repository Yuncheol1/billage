import chromadb
import time
from sentence_transformers import SentenceTransformer
from config import CHROMA_DB_DIR, COLLECTION_NAME, EMBEDDING_MODEL_NAME

def sanity_check():
    print("1. 로컬 Chroma DB 연결...")
    try:
        client = chromadb.PersistentClient(path=CHROMA_DB_DIR)
        collection = client.get_collection(name=COLLECTION_NAME)
    except Exception as e:
        print(f"DB 연결 실패: {e}")
        return

    # 저장된 문서 수 확인
    doc_count = collection.count()
    print(f"   => 현재 저장된 총 문서 수: {doc_count} 개\n")

    print(f"2. 임베딩 모델 로드 중 ({EMBEDDING_MODEL_NAME})...")
    model = SentenceTransformer(EMBEDDING_MODEL_NAME)
    
    # 샘플 질문
    query = "법정 최고 이자율은 얼마인가요?"
    print(f"\n3. 샘플 질문 테스트: '{query}'")

    start_time = time.time()
    
    # 질문 임베딩 생성 (BGE-M3는 질문에도 normalize 적용)
    query_embedding = model.encode([query], normalize_embeddings=True)
    
    # Similarity Search (Top-3 검색)
    results = collection.query(
        query_embeddings=query_embedding.tolist() if hasattr(query_embedding, 'tolist') else [e.tolist() for e in query_embedding],
        n_results=3,
        include=["documents", "metadatas", "distances"]
    )
    
    elapsed = time.time() - start_time
    print(f"\n[검색 완료] 소요 시간(Latency): {elapsed:.4f}초\n")
    
    print("=== Top 3 검색 결과 ===")
    for idx, (doc, meta, dist) in enumerate(zip(results['documents'][0], results['metadatas'][0], results['distances'][0])):
        print(f"[{idx+1}위] {meta['source']} - {meta['article_id']} (Distance: {dist:.4f})")
        print(f"내용: {doc[:150]}...")
        print("-" * 50)

if __name__ == "__main__":
    sanity_check()

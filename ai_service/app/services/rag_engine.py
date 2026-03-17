import os
# ROOT_DIR 설정
import sys
from pathlib import Path
ROOT_DIR = Path(__file__).resolve().parent.parent.parent
sys.path.append(str(ROOT_DIR))

from langchain_community.embeddings import HuggingFaceEmbeddings
from langchain_community.vectorstores import Chroma
from app.core.config import settings

class RAGEngine:
    """
    [EC2 Inference 16GB RAM 최적화 모듈]
    BAAI/bge-m3 모델을 활용하여 ChromaDB 데이터를 검색하는 엔진
    """
    def __init__(self, collection_name: str = "legal_tax_db"):
        self.collection_name = collection_name
        
        # 원칙 4 (자원 최적화 분배): 서버는 GPU가 없는 일반 EC2 16GB RAM
        # device='cpu'로 명시하고, 배치 처리가 필요 없으므로 안정성을 최우선으로 합니다.
        print("💡 [Init] EC2 Inference (16GB RAM) CPU 최적화 모드로 모델을 로딩합니다...")
        self.embedding_model = HuggingFaceEmbeddings(
            model_name="BAAI/bge-m3",
            model_kwargs={
                 'device': 'cpu'  # EC2 타겟
            },
            encode_kwargs={
                 'normalize_embeddings': True
                 # 'batch_size' 옵션은 여기선 한 두개의 유저 질문만 즉시 임베딩하므로 제거/기본값 사용
            }
        )
        
        db_path = os.path.join(ROOT_DIR, settings.CHROMA_DB_DIR)
        os.makedirs(db_path, exist_ok=True)
        self.vector_store = Chroma(
            collection_name=self.collection_name, 
            embedding_function=self.embedding_model,
            persist_directory=db_path
        )
        
    def search_similar_documents(self, query: str, k: int = 3) -> str:
        """
        주어진 질의(query)에 대해 가장 유사한 판례/법령 조항을 k개 반환하고,
        이들을 단일 문자열 컨텍스트(Context)로 결합하여 프롬프트 모듈에 제공
        """
        try:
            # Retriever를 통해 유사도가 높은 문서를 검색 (기본적으로 L2 Distance나 Cosine 사용됨)
            docs = self.vector_store.similarity_search(query, k=k)
            
            if not docs:
                return "관련된 유사 법률/판례 데이터를 벡터 DB에서 찾을 수 없습니다."
            
            # 검색된 문서들의 내용을 줄바꿈으로 이어서 하나의 Context로 구성
            context = "\n\n".join([f"[참고 데이터 {i+1}]: {doc.page_content}" for i, doc in enumerate(docs)])
            return context
            
        except Exception as e:
            return f"법률 DB 검색 중 오류가 발생했습니다: {str(e)}"

# 싱글톤처럼 재사용이 권장되는 서비스 (메모리에 임베딩 모델 로드 시점 조절)
# 필요시 main.py lifespan이나 Dependency Injection을 통해 인스턴스화
rag_engine_instance = None

def get_rag_engine():
    global rag_engine_instance
    if rag_engine_instance is None:
        rag_engine_instance = RAGEngine()
    return rag_engine_instance

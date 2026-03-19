import os
import re
import hashlib
from typing import List, Dict

# 환경 분리 지침(5)에 따라 상대경로 기반으로 환경 설정
# ROOT_DIR 설정
import sys
from pathlib import Path
ROOT_DIR = Path(__file__).resolve().parent
sys.path.append(str(ROOT_DIR))

from langchain_community.embeddings import HuggingFaceEmbeddings
from langchain_community.vectorstores import Chroma
from langchain.docstore.document import Document
from app.core.config import settings

class LegalDataIngester:
    def __init__(self, data_path: str):
        self.data_path = data_path
        
        # 원칙 1 (모델 일관성) 및 원칙 4 (자원 최적화 - 노트북 8GB VRAM용)
        # encode_kwargs에 batch_size 설정으로 처리량 극대화 (최신 sentence-transformers 지원)
        print("💡 [Init] 로컬 8GB VRAM 최적화 모드로 BAAI/bge-m3 모델을 로드합니다...")
        self.embedding_model = HuggingFaceEmbeddings(
            model_name="BAAI/bge-m3",
            model_kwargs={'device': 'cuda'}, # 배포 환경(EC2)에서는 cpu/onnx 등으로 변경 가능
            encode_kwargs={
                'normalize_embeddings': True,
                'batch_size': 32 # 8GB VRAM 활용을 극대화하기 위한 배치 크기
            }
        )
        
        # 원칙 3 (데이터 이동성): Persistent 모드로 지정 디렉토리에 파일로 저장
        self.db_dir = os.path.join(ROOT_DIR, settings.CHROMA_DB_DIR)
        os.makedirs(self.db_dir, exist_ok=True)
        self.vector_store = Chroma(
            collection_name="legal_tax_db",
            embedding_function=self.embedding_model,
            persist_directory=self.db_dir
        )

    def parse_legal_text(self, text: str) -> List[Document]:
        """
        원칙 2 (구조적 청킹 전략): 조항(제N조) 단위로 텍스트를 분리하고 메타데이터 보존
        """
        print("💡 [Chunking] 조항 단위(Article-based)로 문서를 분할합니다...")
        documents = []
        
        # '법 이름' 추출 (대괄호 안의 내용)
        current_law_name = "알 수 없는 법률"
        
        # 정규식 패턴: [법령명] 과 제N조
        law_pattern = re.compile(r'\[(.*?)\]')
        article_pattern = re.compile(r'(제\d+조.*?)(?=제\d+조|\[|$)', re.DOTALL)
        
        # 파일 내용을 법령 단위로 분리 (간이 파싱)
        blocks = text.split('\n\n')
        
        for block in blocks:
            law_match = law_pattern.match(block)
            if law_match:
                current_law_name = law_match.group(1)
                # 법 이름과 함께 있는 본문 조항 분리
                block_text = block[law_match.end():].strip()
            else:
                block_text = block.strip()
                
            if not block_text:
                continue
                
            # '제N조'를 기준으로 텍스트 스플릿
            articles = article_pattern.findall(block_text)
            if not articles:
                # 조문 형태가 아니면 통째로 추가 (가이드라인 등)
                doc = Document(
                    page_content=block_text,
                    metadata={"source": current_law_name, "type": "guideline"}
                )
                documents.append(doc)
            else:
                for article in articles:
                    article = article.strip()
                    if not article: continue
                    # 조항 번호 추출 메타데이터화
                    article_num_match = re.search(r'제(\d+)조', article)
                    article_num = article_num_match.group(1) if article_num_match else "알수없음"
                    
                    doc = Document(
                        page_content=f"[{current_law_name}] {article}",
                        metadata={
                            "source": current_law_name, 
                            "article_num": article_num,
                            "type": "law_article"
                        }
                    )
                    documents.append(doc)
                    
        return documents

    def ingest_data(self):
        with open(self.data_path, 'r', encoding='utf-8') as f:
            text = f.read()
            
        docs = self.parse_legal_text(text)
        print(f"✅ 총 {len(docs)}개의 의미 단위(조항) 문서로 분할되었습니다.")
        
        print(f"🔥 [Ingestion] 배치(Batch) 모드로 Chroma DB 임베딩 및 적재 시작...")
        self.vector_store.add_documents(docs)
        print(f"✅ [Ingestion] DB 적재 완료! 경로: {self.db_dir}")
        self.create_checksum()

    def create_checksum(self):
        """
        원칙 3 (정합성 확인): DB 파일의 무결성을 증명하기 위한 해시(MD5) 생성
        가장 큰 파일(보통 chroma.sqlite3)을 기준으로 생성
        """
        sqlite_path = os.path.join(self.db_dir, 'chroma.sqlite3')
        if os.path.exists(sqlite_path):
            with open(sqlite_path, "rb") as f:
                file_hash = hashlib.md5(f.read()).hexdigest()
            
            checksum_path = os.path.join(self.db_dir, 'checksum.md5')
            with open(checksum_path, "w") as f:
                f.write(f"{file_hash} chroma.sqlite3\n")
            print(f"🔒 [Checksum] 무결성 검증을 위한 MD5 체크섬 생성 완료 ({file_hash})")
            print("📦 EC2 배포 시 'chroma_db' 폴더 전체와 체크섬 파일을 함께 전송하십시오.")

if __name__ == "__main__":
    sample_data_path = os.path.join(ROOT_DIR, "data", "legal_sample.txt")
    ingester = LegalDataIngester(sample_data_path)
    ingester.ingest_data()

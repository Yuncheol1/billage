import os
import glob
from pydantic_settings import BaseSettings, SettingsConfigDict
from typing import List

BASE_DIR = os.path.dirname(os.path.abspath(__file__))

class Settings(BaseSettings):
    # --- 1. 필수 보안 장부 (없으면 서버 구동 즉시 에러 발생 Fail-Fast) ---
    GMS_KEY: str
    GMS_URL: str

    # --- 2. 동적 환경 변수 (기본값 제공, .env 로 오버라이딩 가능) ---
    LLM_MODEL_NAME: str = "gemini-2.0-flash"
    EMBEDDING_MODEL_NAME: str = "BAAI/bge-m3"
    BATCH_SIZE: int = 64
    CHROMA_COLLECTION_NAME: str = "legal_documents"

    # --- 3. 시스템 고정 경로 셋팅 ---
    DATA_DIR: str = os.path.join(BASE_DIR, "data")
    OUTPUT_DIR: str = os.path.join(BASE_DIR, "output")
    CHROMA_DB_DIR: str = os.path.join(BASE_DIR, "chroma_db")
    
    @property
    def PDF_FILES(self) -> List[str]:
        os.makedirs(self.DATA_DIR, exist_ok=True)
        os.makedirs(self.OUTPUT_DIR, exist_ok=True)
        return [os.path.basename(p) for p in glob.glob(os.path.join(self.DATA_DIR, "*.pdf"))]
        
    @property
    def GMS_BASE_URL(self) -> str:
        return f"{self.GMS_URL.rstrip('/')}/models/{self.LLM_MODEL_NAME}:generateContent"

    # [핵심] Pydantic이 서버 최상단의 .env를 자동으로 스캔하도록 맵핑
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")

# 서버(또는 모듈) 가동 시 1회만 호출되어 전체 값 검증 완료 (Singleton)
config = Settings()

# --- 하위 호환성 래핑 (기존 스크립트 수정 없이 그대로 사용하도록 배려) ---
DATA_DIR = config.DATA_DIR
OUTPUT_DIR = config.OUTPUT_DIR
CHROMA_DB_DIR = config.CHROMA_DB_DIR
PDF_FILES = config.PDF_FILES

EMBEDDING_MODEL_NAME = config.EMBEDDING_MODEL_NAME
BATCH_SIZE = config.BATCH_SIZE
COLLECTION_NAME = config.CHROMA_COLLECTION_NAME

GMS_API_KEY = config.GMS_KEY
GMS_BASE_URL = config.GMS_BASE_URL

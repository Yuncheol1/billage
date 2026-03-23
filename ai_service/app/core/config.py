import os
# 멀티스레드 데드락(무한 대기) 방지 환경 변수를 파일 최상단에 강제 주입
os.environ["TOKENIZERS_PARALLELISM"] = "false"
os.environ["OMP_NUM_THREADS"] = "1"

from pydantic_settings import BaseSettings, SettingsConfigDict
import os

class Settings(BaseSettings):
    PROJECT_NAME: str = "Secure AI Microservice API"
    API_V1_STR: str = "/internal/v1"
    
    # 보안 (Server to Server Auth)
    INTERNAL_API_KEY: str = ""
    
    # AI 환경변수 (SSAFY GMS Gateway 전용)
    GMS_KEY: str = ""
    GMS_URL: str = "https://gms.ssafy.io/gmsapi/generativelanguage.googleapis.com/v1beta"
    
    CHROMA_DB_DIR: str = "./chroma_db"
    
    # Redis 설정 (Semantic Cache 전용)
    REDIS_HOST: str = "172.26.2.14" # EC2의 Private IP (eth0)
    REDIS_PORT: int = 6379
    REDIS_DB: int = 1
    SEMANTIC_CACHE_THRESHOLD: float = 0.95  # 유사도 임계값
    
    # .env 파일의 위치를 config.py 기준으로 절대 경로 설정 (어디서 실행해도 로드 가능)
    model_config = SettingsConfigDict(
        env_file=os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), ".env"),
        env_file_encoding="utf-8",
        extra="ignore"
    )

settings = Settings()

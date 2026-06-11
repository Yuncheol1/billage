import os
# 멀티스레드 데드락(무한 대기) 방지 환경 변수를 파일 최상단에 강제 주입
os.environ["TOKENIZERS_PARALLELISM"] = "false"
os.environ["OMP_NUM_THREADS"] = "1"

from pydantic_settings import BaseSettings, SettingsConfigDict
from pydantic import Field, field_validator

class Settings(BaseSettings):
    PROJECT_NAME: str = "Secure AI Microservice API"
    API_V1_STR: str = "/internal/v1"
    
    # 보안 (Server to Server Auth)
    INTERNAL_API_KEY: str = ""
    
    # AI 환경변수 (SSAFY GMS Gateway 전용)
    GMS_KEY: str = ""
    GMS_URL: str = ""
    
    CHROMA_DB_DIR: str = "./chroma_db"
    
    # Redis 설정 (Semantic Cache 전용)
    REDIS_HOST: str = "billage-redis" # Docker Link를 통한 내부망 통신 호스트명
    REDIS_PORT: int = 6379
    REDIS_DB: int = 1
    SEMANTIC_CACHE_THRESHOLD: float = 0.95  # 유사도 임계값
    ALLOWED_ORIGINS: list[str] = Field(default_factory=lambda: ["*"])
    GMS_TIMEOUT_SECONDS: float = 50.0
    GMS_CONNECT_TIMEOUT_SECONDS: float = 10.0
    GMS_MAX_RETRIES: int = 3
    GMS_BASE_BACKOFF_SECONDS: float = 1.0
    MAX_USER_INTENT_LENGTH: int = 2000
    MAX_CHAT_MESSAGES: int = 200
    MAX_CHAT_MESSAGE_LENGTH: int = 4000
    
    # .env 파일의 위치를 config.py 기준으로 절대 경로 설정 (어디서 실행해도 로드 가능)
    model_config = SettingsConfigDict(
        env_file=os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), ".env"),
        env_file_encoding="utf-8",
        extra="ignore"
    )

    @field_validator("ALLOWED_ORIGINS", mode="before")
    @classmethod
    def parse_allowed_origins(cls, value):
        if value is None or value == "":
            return ["*"]
        if isinstance(value, str):
            return [origin.strip() for origin in value.split(",") if origin.strip()]
        return value

settings = Settings()

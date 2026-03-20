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
    
    # .env 파일의 위치를 config.py 기준으로 절대 경로 설정 (어디서 실행해도 로드 가능)
    model_config = SettingsConfigDict(
        env_file=os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), ".env"),
        env_file_encoding="utf-8",
        extra="ignore"
    )

settings = Settings()

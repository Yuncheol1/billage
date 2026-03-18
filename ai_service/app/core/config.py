from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    PROJECT_NAME: str = "Secure AI Microservice API"
    API_V1_STR: str = "/internal/v1"
    
    # 보안 (Server to Server Auth)
    INTERNAL_API_KEY: str = "super-secret-key-change-me-in-production"
    
    # AI 환경변수 (SSAFY GMS Gateway 전용)
    GMS_KEY: str = ""
    GMS_URL: str = "https://gms.ssafy.io/gmsapi/generativelanguage.googleapis.com/v1beta"
    
    CHROMA_DB_DIR: str = "./chroma_db"
    
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

settings = Settings()

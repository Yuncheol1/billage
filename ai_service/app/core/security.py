from fastapi import Security, HTTPException, status
from fastapi.security import APIKeyHeader
from app.core.config import settings

api_key_header = APIKeyHeader(name="X-Internal-API-Key", auto_error=False)

async def verify_internal_api_key(api_key_header: str = Security(api_key_header)):
    """
    Spring 서버에서 보낸 요청인지 확인하는 내부망 전용 API KEY 검증 로직
    클라이언트가 아닌 오직 허가된 백엔드 서버에서만 호출 가능토록 합니다.
    """
    if api_key_header == settings.INTERNAL_API_KEY:
        return True
    
    raise HTTPException(
        status_code=status.HTTP_403_FORBIDDEN,
        detail="Forbidden: Invalid Internal API Key",
    )

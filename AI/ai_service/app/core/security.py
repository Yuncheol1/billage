import logging
import secrets
from fastapi import Security, HTTPException, status
from fastapi.security import APIKeyHeader
from app.core.config import settings

logger = logging.getLogger(__name__)
api_key_header = APIKeyHeader(name="X-Internal-API-Key", auto_error=False)


def ensure_security_settings():
    """운영에 필요한 보안 설정이 비어 있으면 서버 시작 단계에서 차단합니다."""
    if not settings.INTERNAL_API_KEY.strip():
        logger.error("INTERNAL_API_KEY is not configured.")
        raise RuntimeError("INTERNAL_API_KEY must be configured before starting the server.")

async def verify_internal_api_key(api_key_header: str = Security(api_key_header)):
    """
    Spring 서버에서 보낸 요청인지 확인하는 내부망 전용 API KEY 검증 로직
    클라이언트가 아닌 오직 허가된 백엔드 서버에서만 호출 가능토록 합니다.
    """
    expected_api_key = settings.INTERNAL_API_KEY.strip()
    if api_key_header and expected_api_key and secrets.compare_digest(api_key_header, expected_api_key):
        return True
    
    raise HTTPException(
        status_code=status.HTTP_403_FORBIDDEN,
        detail="Forbidden: Invalid Internal API Key",
    )

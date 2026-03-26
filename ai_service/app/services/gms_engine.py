import httpx
from typing import List, Dict, Any
from app.core.config import settings
import asyncio
import random
import logging

# 로깅 설정
logger = logging.getLogger(__name__)
_shared_async_client: httpx.AsyncClient | None = None


def _build_timeout() -> httpx.Timeout:
    return httpx.Timeout(
        timeout=settings.GMS_TIMEOUT_SECONDS,
        connect=settings.GMS_CONNECT_TIMEOUT_SECONDS,
    )


def get_gms_client() -> httpx.AsyncClient:
    global _shared_async_client
    if _shared_async_client is None or _shared_async_client.is_closed:
        _shared_async_client = httpx.AsyncClient(timeout=_build_timeout())
    return _shared_async_client


async def close_gms_client() -> None:
    global _shared_async_client
    if _shared_async_client is not None and not _shared_async_client.is_closed:
        await _shared_async_client.aclose()
    _shared_async_client = None

class GMSGeminiClient:
    """
    SSAFY GMS 플랫폼(Proxy)을 경유하여 Gemini 모델과 통신하는 커스텀 비동기 HTTP 클라이언트.
    429 (Too Many Requests) 에러 발생 시 지수 백오프(Exponential Backoff) 재시도를 수행합니다.
    """
    def __init__(self, model_name: str = "gemini-2.0-flash"):
        self.model_name = model_name
        self.api_url = f"{settings.GMS_URL}/models/{self.model_name}:generateContent"
        self.max_retries = settings.GMS_MAX_RETRIES

    async def _request_with_retry(self, headers: dict, payload: dict) -> dict:
        """API 호출 및 429(할당량 초과) 발생 시 지수 백오프 기반 재시도 로직"""
        client = get_gms_client()
        for attempt in range(self.max_retries + 1):
            try:
                response = await client.post(self.api_url, headers=headers, json=payload)

                if response.status_code == 429:
                    if attempt == self.max_retries:
                        logger.error("GMS API maximum retry count exceeded for 429 response.")
                        response.raise_for_status()

                    wait_time = (settings.GMS_BASE_BACKOFF_SECONDS * (2 ** attempt)) + random.uniform(0.1, 0.5)
                    logger.warning(
                        "GMS rate limited: status=429 attempt=%s/%s wait=%.2fs model=%s",
                        attempt + 1,
                        self.max_retries,
                        wait_time,
                        self.model_name,
                    )
                    await asyncio.sleep(wait_time)
                    continue

                response.raise_for_status()
                return response.json()

            except httpx.HTTPStatusError as e:
                status_code = e.response.status_code if e.response is not None else "unknown"
                if status_code in {429, 500, 502, 503, 504} and attempt < self.max_retries:
                    wait_time = (settings.GMS_BASE_BACKOFF_SECONDS * (2 ** attempt)) + random.uniform(0.1, 0.5)
                    logger.warning(
                        "GMS HTTP error: status=%s attempt=%s/%s wait=%.2fs model=%s",
                        status_code,
                        attempt + 1,
                        self.max_retries,
                        wait_time,
                        self.model_name,
                    )
                    await asyncio.sleep(wait_time)
                    continue

                logger.error(
                    "GMS HTTP error without recovery: status=%s model=%s detail=%s",
                    status_code,
                    self.model_name,
                    str(e),
                )
                raise
            except httpx.RequestError as e:
                if attempt < self.max_retries:
                    wait_time = (settings.GMS_BASE_BACKOFF_SECONDS * (2 ** attempt)) + random.uniform(0.1, 0.5)
                    logger.warning(
                        "GMS network error: attempt=%s/%s wait=%.2fs model=%s detail=%s",
                        attempt + 1,
                        self.max_retries,
                        wait_time,
                        self.model_name,
                        str(e),
                    )
                    await asyncio.sleep(wait_time)
                    continue

                logger.error("GMS network error without recovery: model=%s detail=%s", self.model_name, str(e))
                raise
            except Exception as e:
                logger.exception("Unexpected GMS client error: model=%s", self.model_name)
                raise
        raise Exception("API 호출 재시도 횟수를 초과했습니다.")

    async def generate_response(self, system_instruction: str, user_prompt: str) -> str:
        """단일 턴 프롬프트 (분석용, 특약용)"""
        headers = {
            "Content-Type": "application/json",
            "x-goog-api-key": settings.GMS_KEY
        }
        
        payload = {
            "system_instruction": {"parts": [{"text": system_instruction}]},
            "contents": [{"role": "user", "parts": [{"text": user_prompt}]}],
            "generationConfig": {"temperature": 0.1}
        }
        
        data = await self._request_with_retry(headers, payload)
        
        try:
            return data["candidates"][0]["content"]["parts"][0]["text"]
        except (KeyError, IndexError):
            return "AI 응답을 파싱할 수 없습니다."

    async def generate_chat(self, history: List[Dict[str, Any]], current_message: str, system_instruction: str = None) -> str:
        """멀티 턴 챗봇용"""
        headers = {
            "Content-Type": "application/json",
            "x-goog-api-key": settings.GMS_KEY
        }
        
        contents = []
        for msg in history:
            role = "model" if msg["role"] == "assistant" else "user"
            contents.append({"role": role, "parts": [{"text": msg["content"]}]})
            
        contents.append({"role": "user", "parts": [{"text": current_message}]})
        
        if system_instruction is None:
            system_instruction = "당신은 전문 법률/세무 챗봇입니다. 정확하고 친절하게 답변하세요."
        
        payload = {
            "system_instruction": {"parts": [{"text": system_instruction}]},
            "contents": contents,
            "generationConfig": {"temperature": 0.1}
        }
        
        data = await self._request_with_retry(headers, payload)
        
        try:
            return data["candidates"][0]["content"]["parts"][0]["text"]
        except (KeyError, IndexError):
            return "AI 응답을 파싱할 수 없습니다."

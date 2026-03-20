import httpx
from typing import List, Dict, Any
from app.core.config import settings
import asyncio
import random
import logging

# 로깅 설정
logger = logging.getLogger(__name__)

class GMSGeminiClient:
    """
    SSAFY GMS 플랫폼(Proxy)을 경유하여 Gemini 모델과 통신하는 커스텀 비동기 HTTP 클라이언트.
    429 (Too Many Requests) 에러 발생 시 지수 백오프(Exponential Backoff) 재시도를 수행합니다.
    """
    def __init__(self, model_name: str = "gemini-2.0-flash"):
        self.model_name = model_name
        self.api_url = f"{settings.GMS_URL}/models/{self.model_name}:generateContent"
        self.max_retries = 3  # 최대 3회 재시도
        
    async def _request_with_retry(self, headers: dict, payload: dict) -> dict:
        """API 호출 및 429 발생 시 재시도 로직"""
        for attempt in range(self.max_retries + 1):
            async with httpx.AsyncClient() as client:
                try:
                    response = await client.post(self.api_url, headers=headers, json=payload, timeout=45.0)
                    
                    # 429 Too Many Requests 발생 시 
                    if response.status_code == 429:
                        if attempt == self.max_retries:
                            response.raise_for_status()
                        
                        # 지수 백오프 적용 (2, 4, 8초 ... + 약간의 지터)
                        wait_time = (2 ** attempt) + random.uniform(0, 1)
                        logger.warning(f"⚠️ GMS API 할당량 초과 (429). {wait_time:.1f}초 후 재시도합니다... (시도 {attempt+1}/{self.max_retries})")
                        await asyncio.sleep(wait_time)
                        continue
                        
                    response.raise_for_status()
                    return response.json()
                    
                except httpx.HTTPStatusError as e:
                    if e.response.status_code != 429 or attempt == self.max_retries:
                        logger.error(f"❌ GMS API 호출 에러: {e}")
                        raise e
                except Exception as e:
                    if attempt == self.max_retries:
                        logger.error(f"❌ 예상치 못한 에러: {e}")
                        raise e
                    await asyncio.sleep(1)
        
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

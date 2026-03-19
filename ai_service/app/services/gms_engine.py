import httpx
from typing import List, Dict, Any
from app.core.config import settings
import json

class GMSGeminiClient:
    """
    SSAFY GMS 플랫폼(Proxy)을 경유하여 Gemini 모델과 통신하는 커스텀 비동기 HTTP 클라이언트.
    LangChain의 기본 GoogleGenerativeAI가 커스텀 헤더/경로와 충돌하는 것을 방지하기 위해 직접 구현합니다.
    """
    def __init__(self, model_name: str = "gemini-2.0-flash"):
        self.model_name = model_name
        self.api_url = f"{settings.GMS_URL}/models/{self.model_name}:generateContent"
        
    async def generate_response(self, system_instruction: str, user_prompt: str) -> str:
        """단일 턴 프롬프트 (분석용, 특약용)"""
        headers = {
            "Content-Type": "application/json",
            "x-goog-api-key": settings.GMS_KEY
        }
        
        # System Instruction을 사용하는 Gemini 포맷 (v1beta 기준)
        payload = {
            "system_instruction": {
                "parts": [{"text": system_instruction}]
            },
            "contents": [
                {
                    "role": "user",
                    "parts": [{"text": user_prompt}]
                }
            ],
            "generationConfig": {
                "temperature": 0.1, # 법률 도메인이므로 환각을 막기 위해 0.1 등 낮게 설정
            }
        }
        
        async with httpx.AsyncClient() as client:
            response = await client.post(self.api_url, headers=headers, json=payload, timeout=30.0)
            response.raise_for_status()
            data = response.json()
            
            # 응답 결과 파싱
            try:
                msg = data["candidates"][0]["content"]["parts"][0]["text"]
                return msg
            except (KeyError, IndexError):
                return "AI 응답을 파싱할 수 없습니다."

    async def generate_chat(self, history: List[Dict[str, Any]], current_message: str, system_instruction: str = None) -> str:
        """멀티 턴 챗봇용. system_instruction을 외부에서 주입 가능."""
        headers = {
            "Content-Type": "application/json",
            "x-goog-api-key": settings.GMS_KEY
        }
        
        # 히스토리 변환 (LangChain 형식 또는 커스텀 형식 -> Gemini 포맷)
        contents = []
        for msg in history:
            role = "model" if msg["role"] == "assistant" else "user"
            contents.append({
                "role": role,
                "parts": [{"text": msg["content"]}]
            })
            
        # 현재 유저 메시지 추가
        contents.append({
            "role": "user",
            "parts": [{"text": current_message}]
        })
        
        # 시스템 지시문: 외부 주입이 없으면 기본값 사용
        if system_instruction is None:
            system_instruction = "당신은 차용증 작성 및 세금(증여세, 이자소득세 등) 관련 전문 법률/세무 챗봇입니다. 정확하고 친절하게 답변하세요."
        
        payload = {
            "system_instruction": {
                "parts": [{"text": system_instruction}]
            },
            "contents": contents,
            "generationConfig": {
                "temperature": 0.1,  # 법률 도메인이므로 환각 방지를 위해 낮게 설정
            }
        }
        
        async with httpx.AsyncClient() as client:
            response = await client.post(self.api_url, headers=headers, json=payload, timeout=30.0)
            response.raise_for_status()
            data = response.json()
            
            try:
                msg = data["candidates"][0]["content"]["parts"][0]["text"]
                return msg
            except (KeyError, IndexError):
                return "AI 응답을 파싱할 수 없습니다."

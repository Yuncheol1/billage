from pydantic import BaseModel, Field
from typing import List, Optional

class ChatMessage(BaseModel):
    role: str = Field(..., description="'user' 또는 'assistant'")
    content: str = Field(..., description="메시지 내용")

# --- Request ---
class ChatRequest(BaseModel):
    message: str = Field(..., description="사용자의 현재 질문")
    history: List[ChatMessage] = Field(default_factory=list, description="이전 대화 내역 (Context 유지용)")
    contract_seq: Optional[int] = Field(None, description="특정 계약 맥락의 질문일 때 전달")

# --- Response ---
class ChatResponse(BaseModel):
    reply: str = Field(..., description="RAG 기반 전문 답변")

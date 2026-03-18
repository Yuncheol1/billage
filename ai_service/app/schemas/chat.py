from pydantic import BaseModel, Field
from typing import List

class ChatMessage(BaseModel):
    role: str = Field(..., description="'user' 또는 'assistant'")
    content: str = Field(..., description="메시지 내용")

class ChatRequest(BaseModel):
    message: str = Field(..., description="사용자의 현재 질문")
    history: List[ChatMessage] = Field(default_factory=list, description="이전 대화 내역 (Context 유지용)")

class ChatResponse(BaseModel):
    reply: str = Field(..., description="챗봇 AI(RAG 기반) 답변")

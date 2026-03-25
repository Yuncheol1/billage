from pydantic import BaseModel, Field
from typing import List, Optional


class ChatMessageItem(BaseModel):
    """Spring이 MongoDB에서 조회 후 가공하여 전달하는 채팅 메시지 단위"""
    sender_seq: int = Field(..., description="발신자 사용자 식별자 (senderSeq)")
    content: str = Field(..., description="메시지 내용")
    created_at: Optional[str] = Field(None, description="메시지 전송 시간 (ISO 8601)")
    msg_type: Optional[str] = Field("TEXT", description="메시지 유형 (TEXT / AI_PROPOSAL 등)")


# --- Request ---
class ClauseChatRequest(BaseModel):
    contract_seq: Optional[int] = Field(None, description="소속 계약 식별자 (contract.contract_seq)")
    room_id: Optional[str] = Field(None, description="채팅방 ID (추적/로깅용)")
    template_type: Optional[str] = Field(None, description="계약 유형 (FRIEND_SMALL / FAMILY_JEONSE 등)")
    contract_context: str = Field(default="", description="현재까지 작성된 차용증 문맥")
    chat_messages: List[ChatMessageItem] = Field(..., description="두 사용자 간 채팅 메시지 배열")


# --- Response ---
class ClauseItem(BaseModel):
    """contract_clause 테이블에 INSERT 가능한 특약 구조체"""
    clause_id: Optional[str] = Field(None, description="조항 유형 식별자 (PENALTY/COLLATERAL/INTEREST/GUARANTEE/OTHER)")
    title: Optional[str] = Field(None, description="조항 제목")
    legal_clause: str = Field(..., description="법적으로 정제된 특약사항 본문 (contract_clause.content)")
    explanation: str = Field(default="", description="해당 조항의 법적 의미 설명")


class ClauseChatResponse(BaseModel):
    clauses: List[ClauseItem] = Field(default_factory=list, description="생성된 특약사항 배열")
    summary: str = Field(default="", description="대화 분석 요약")

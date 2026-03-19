from pydantic import BaseModel, Field
from typing import Optional

# --- Request ---
class ClauseGenerateRequest(BaseModel):
    contract_seq: Optional[int] = Field(None, description="소속 계약 식별자 (contract.contract_seq)")
    user_intent: str = Field(..., description="자연어 의도 (예: '돈 안 갚으면 차 뺏을 거야')")
    contract_context: str = Field(default="", description="현재까지 작성된 차용증 문맥 (옵션)")
    template_type: Optional[str] = Field(None, description="계약 유형 (맥락 보강용)")

# --- Response: contract_clause 테이블에 바로 INSERT 가능한 구조 ---
class ClauseGenerateResponse(BaseModel):
    clause_id: Optional[str] = Field(None, description="생성된 조항 유형 식별자 (contract_clause.clause_id)")
    title: Optional[str] = Field(None, description="조항 제목 (contract_clause.title)")
    legal_clause: str = Field(..., description="법적으로 다듬어진 특약사항 본문 (contract_clause.content)")
    explanation: str = Field(..., description="해당 특약의 법적 효력 설명")

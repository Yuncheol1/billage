from pydantic import BaseModel, Field

class ClauseGenerateRequest(BaseModel):
    user_intent: str = Field(..., description="특약사항으로 넣고 싶은 사용자의 자연어 의도 (예: 안 갚으면 차 뺏음)")
    contract_context: str = Field(default="", description="현재까지 작성된 차용증 문맥 (옵션)")

class ClauseGenerateResponse(BaseModel):
    legal_clause: str = Field(..., description="법적으로 유효하게 다듬어진 특약사항 문장")
    explanation: str = Field(..., description="해당 특약사항이 가지는 법적 효력 설명")

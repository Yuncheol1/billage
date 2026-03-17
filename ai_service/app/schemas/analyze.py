from pydantic import BaseModel, Field
from typing import List, Optional

class AnalyzeRequest(BaseModel):
    contract_text: str = Field(..., description="차용증 전체 또는 부분 텍스트")
    mode: str = Field(default="full", description="분석 모드: 'full' (전체) 또는 'partial' (조항/부분)")
    principal_amount: Optional[int] = Field(None, description="대여 원금 (명시적 전달 시 세금 계산 보조)")
    interest_rate: Optional[float] = Field(None, description="연 이자율 (명시적 전달 시)")

class WarningItem(BaseModel):
    risk_level: str = Field(..., description="위험도 (HIGH, MEDIUM, LOW)")
    category: str = Field(..., description="분류 (TOXIC_CLAUSE, TAX_ISSUE, MISSING_INFO)")
    reason: str = Field(..., description="위험 사유 (판례나 세법에 근거한 설명)")
    suggestion: str = Field(..., description="대안 및 수정 권고안")

class AnalyzeResponse(BaseModel):
    is_safe: bool = Field(..., description="전체적으로 서명해도 안전한지 여부")
    warnings: List[WarningItem] = Field(default_factory=list, description="탐지된 위험/독소조항 리스트")
    summary: str = Field(..., description="분석 결과 총평")

from pydantic import BaseModel, Field
from typing import List, Optional

# --- contract_clause 테이블 매핑 ---
class ClauseItem(BaseModel):
    """Spring이 contract_clause 테이블에서 조회하여 전달하는 개별 조항"""
    clause_id: Optional[str] = Field(None, description="조항 유형 식별자 (INTEREST/REPAYMENT_DATE/PENALTY)")
    title: Optional[str] = Field(None, description="조항 제목")
    content: str = Field(..., description="조항 본문")


# --- Request ---
class AnalyzeRequest(BaseModel):
    contract_seq: Optional[int] = Field(None, description="contract.contract_seq (추적/로깅용)")
    contract_text: str = Field(..., description="차용증 전체 또는 부분 텍스트 (Spring이 조합하여 전달)")
    mode: str = Field(default="full", description="분석 모드: 'full' (전체) / 'partial' (조항 단위)")
    template_type: Optional[str] = Field(None, description="FRIEND_SMALL / FAMILY_JEONSE / PARTNER_BIZ / COLLEAGUE_EMERGENCY")
    principal_amount: Optional[float] = Field(None, description="대여 원금 (contract.principal_amount)")
    interest_rate: Optional[float] = Field(None, description="연 이자율 소수 표기 (0.046 = 4.6%) (contract.interest_rate)")
    repayment_method: Optional[str] = Field(None, description="BULLET / INSTALLMENT / CUSTOM")
    start_dt: Optional[str] = Field(None, description="대출 시작일 (YYYY-MM-DD)")
    end_dt: Optional[str] = Field(None, description="대출 종료일 (YYYY-MM-DD)")
    clauses: Optional[List[ClauseItem]] = Field(default_factory=list, description="contract_clause 테이블에서 가져온 개별 조항 리스트")


# --- Response ---
class WarningItem(BaseModel):
    risk_level: str = Field(..., description="위험도 (HIGH, MEDIUM, LOW)")
    category: str = Field(..., description="분류 (TOXIC_CLAUSE, TAX_ISSUE, MISSING_INFO)")
    clause_id: Optional[str] = Field(None, description="문제가 된 조항 식별자 (contract_clause.clause_id 매핑용)")
    reason: str = Field(..., description="판례/세법에 근거한 위험 사유")
    suggestion: str = Field(..., description="수정 권고안")

class TaxSummary(BaseModel):
    """tax_calculation 테이블 저장용 세금 요약 정보"""
    gift_tax_risk: bool = Field(False, description="증여세 추징 위험 여부")
    estimated_gift_tax: Optional[float] = Field(None, description="추정 증여세액")
    withholding_tax_rate: Optional[str] = Field(None, description="원천징수 세율 안내 (예: 27.5%)")

class AnalyzeResponse(BaseModel):
    is_safe: bool = Field(..., description="서명해도 안전한지 여부")
    warnings: List[WarningItem] = Field(default_factory=list, description="탐지된 위험 항목 리스트")
    tax_summary: Optional[TaxSummary] = Field(None, description="세금 관련 요약 (tax_calculation 테이블 저장용)")
    summary: str = Field(..., description="분석 결과 총평")

from fastapi import APIRouter
from app.schemas.analyze import AnalyzeRequest, AnalyzeResponse, WarningItem, TaxSummary
from app.services.analyzer import AnalyzeService

router = APIRouter()
analyze_service = AnalyzeService()

# review_result 유효 값
VALID_REVIEW_RESULTS = {"SAFE", "TAX_RISK_ONLY", "LEGAL_VIOLATION", "BOTH"}

# 세무 관련 키워드 (fallback 분류용)
TAX_KEYWORDS = {"증여세", "상속세", "원천징수", "세율", "세금", "과세", "증여", "납세"}


def _classify_review_result(warnings: list[WarningItem]) -> str:
    """warnings 내용 기반 review_result fallback 분류"""
    if not warnings:
        return "SAFE"

    has_tax = False
    has_legal = False

    for w in warnings:
        # reason + suggestion 텍스트에서 세무 키워드 탐지
        combined_text = w.reason + " " + w.suggestion
        if any(kw in combined_text for kw in TAX_KEYWORDS):
            has_tax = True
        else:
            has_legal = True

    if has_tax and has_legal:
        return "BOTH"
    elif has_tax:
        return "TAX_RISK_ONLY"
    else:
        return "LEGAL_VIOLATION"


@router.post("", response_model=AnalyzeResponse)
async def analyze_contract(request: AnalyzeRequest):
    """
    [기능 1] RAG 기반 차용증 독소조항 판단 및 세금 위험도 분석
    Spring이 contract + contract_clause 테이블 데이터를 조합하여 전달
    """
    result_dict = await analyze_service.analyze_document(request)

    # warnings 파싱
    warnings = [WarningItem(**w) for w in result_dict.get("warnings", [])]

    # tax_summary 파싱 (선택적 응답)
    tax_summary_data = result_dict.get("tax_summary")
    tax_summary = TaxSummary(**tax_summary_data) if tax_summary_data else None

    # review_result: LLM 응답 우선, 유효하지 않으면 키워드 기반 fallback
    llm_review = result_dict.get("review_result", "")
    if llm_review in VALID_REVIEW_RESULTS:
        review_result = llm_review
    else:
        review_result = _classify_review_result(warnings)

    return AnalyzeResponse(
        is_safe=result_dict.get("is_safe", False),
        review_result=review_result,
        warnings=warnings,
        tax_summary=tax_summary,
        summary=result_dict.get("summary", "분석 완료")
    )

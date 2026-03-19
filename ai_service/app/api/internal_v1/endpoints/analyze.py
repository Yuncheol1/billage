from fastapi import APIRouter
from app.schemas.analyze import AnalyzeRequest, AnalyzeResponse, WarningItem, TaxSummary
from app.services.analyzer import AnalyzeService

router = APIRouter()
analyze_service = AnalyzeService()

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
    
    return AnalyzeResponse(
        is_safe=result_dict.get("is_safe", False),
        warnings=warnings,
        tax_summary=tax_summary,
        summary=result_dict.get("summary", "분석 완료")
    )

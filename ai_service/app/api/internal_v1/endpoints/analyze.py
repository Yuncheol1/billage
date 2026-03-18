from fastapi import APIRouter
from app.schemas.analyze import AnalyzeRequest, AnalyzeResponse, WarningItem
from app.services.analyzer import AnalyzeService

router = APIRouter()
analyze_service = AnalyzeService()

@router.post("", response_model=AnalyzeResponse)
async def analyze_contract(request: AnalyzeRequest):
    """
    [기능 1] RAG 기반 차용증 독소조항 판단 및 세금 위험도 분석
    (유저가 전체 폼을 작성했거나, 특정 항목만 적었을 때 모두 대응)
    """
    result_dict = await analyze_service.analyze_document(
        text=request.contract_text,
        mode=request.mode,
        principal=request.principal_amount,
        interest=request.interest_rate
    )
    
    warnings = [WarningItem(**w) for w in result_dict.get("warnings", [])]
    
    return AnalyzeResponse(
        is_safe=result_dict.get("is_safe", False),
        warnings=warnings,
        summary=result_dict.get("summary", "분석 완료")
    )

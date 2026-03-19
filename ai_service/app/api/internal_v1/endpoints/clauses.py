from fastapi import APIRouter
from app.schemas.clause import ClauseGenerateRequest, ClauseGenerateResponse
from app.services.clause_maker import ClauseMakerService

router = APIRouter()
clause_service = ClauseMakerService()

@router.post("/generate", response_model=ClauseGenerateResponse)
async def generate_clause(request: ClauseGenerateRequest):
    """
    [기능 2] 사용자의 자연어를 바탕으로 법적으로 유효한 특약사항 생성
    응답은 contract_clause 테이블에 바로 INSERT 가능한 구조
    """
    result_dict = await clause_service.generate_legal_clause(
        user_intent=request.user_intent,
        context=request.contract_context,
        template_type=request.template_type
    )
    
    return ClauseGenerateResponse(
        clause_id=result_dict.get("clause_id"),
        title=result_dict.get("title"),
        legal_clause=result_dict.get("legal_clause", ""),
        explanation=result_dict.get("explanation", "")
    )

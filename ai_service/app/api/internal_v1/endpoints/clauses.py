from fastapi import APIRouter
from app.schemas.clause import ClauseGenerateRequest, ClauseGenerateResponse
from app.schemas.clause_chat import ClauseChatRequest, ClauseChatResponse, ClauseItem
from app.services.clause_maker import ClauseMakerService
from app.services.clause_chat_service import ClauseChatService

router = APIRouter()
clause_service = ClauseMakerService()
clause_chat_service = ClauseChatService()

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


@router.post("/from-chat", response_model=ClauseChatResponse)
async def generate_clauses_from_chat(request: ClauseChatRequest):
    """
    [기능 4] 두 사용자 간 채팅 내역을 분석하여 법적 특약사항을 자동 생성.
    Spring이 MongoDB에서 조회·가공한 채팅 메시지 배열을 입력으로 받습니다.
    대화에서 합의된 모든 조건을 복수의 특약으로 한번에 반환합니다.
    """
    # 채팅 메시지를 dict 배열로 변환 (Pydantic → dict)
    messages_dicts = [msg.model_dump() for msg in request.chat_messages]

    result = await clause_chat_service.generate_clauses_from_chat(
        chat_messages=messages_dicts,
        contract_context=request.contract_context,
        template_type=request.template_type,
    )

    # 응답 구조체 조립
    clause_items = [
        ClauseItem(
            clause_id=c.get("clause_id"),
            title=c.get("title"),
            legal_clause=c.get("legal_clause", ""),
            explanation=c.get("explanation", ""),
        )
        for c in result.get("clauses", [])
    ]

    return ClauseChatResponse(
        clauses=clause_items,
        summary=result.get("summary", ""),
    )

from fastapi import APIRouter
from app.schemas.chat import ChatRequest, ChatResponse
from app.services.chatbot import ChatbotService

router = APIRouter()
chatbot_service = ChatbotService()

@router.post("", response_model=ChatResponse)
async def chat_interaction(request: ChatRequest):
    """
    [기능 3] 세법 및 차용증 작성과 관련된 대화형 Q&A 챗봇
    (스트리밍 없이 한 번에 JSON으로 응답하는 기본 구조)
    """
    # dict 형태의 history 리스트 변환 (BaseModel -> dict)
    history_dicts = [h.model_dump() for h in request.history]
    
    reply = await chatbot_service.get_chat_response(history_dicts, request.message)
    
    return ChatResponse(reply=reply)

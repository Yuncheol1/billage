from app.services.gms_engine import GMSGeminiClient
from app.services.rag_engine import get_rag_engine

class ChatbotService:
    def __init__(self):
        self.llm_client = GMSGeminiClient()
        self.rag_engine = get_rag_engine()
        
    async def get_chat_response(self, history: list, message: str) -> str:
        """
        [기능 3] 세법 및 법률 상담 챗봇 파이프라인
        """
        
        # 1. 사용자의 질문을 기반으로 RAG 검색
        rag_context = self.rag_engine.search_similar_documents(message, k=2)
        
        # 2. 검색 내용을 프롬프트 컨텍스트로 보강하여 주입
        enhanced_message = f"[사용자 질의]: {message}\n\n[RAG 검색된 관련 법률 기반 사실(참고용)]\n{rag_context}"
        
        # 3. 히스토리와 보강된 메시지를 GMS 클라이언트의 채팅 모드에 호출
        ai_reply = await self.llm_client.generate_chat(history, enhanced_message)
        return ai_reply

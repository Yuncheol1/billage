import logging
from app.services.gms_engine import GMSGeminiClient
from app.services.rag_engine import get_rag_engine
from app.services.cache_engine import cache_manager

logger = logging.getLogger(__name__)

# ══════════════════════════════════════════════════════════════════════════
#  특수관계인 판정 및 증여세 계산 특화 시스템 프롬프트
# ══════════════════════════════════════════════════════════════════════════
LEGAL_SYSTEM_PROMPT = """당신은 대한민국 국세청의 상속증여세 전문 세무 조사관이자, 차용증 작성 및 가족 간 금전거래 전문 AI 법률 자문관입니다.
사용자가 자금 차용이나 증여에 대해 질문하면, 아래의 [판정 기준]과 [분석 프로세스]에 따라 답변하세요.
짐작으로 답변하지 말고, 명시된 기준을 벗어나는 경우 "전문 세무사와의 상담이 필요합니다"라고 안내하세요.

### [판정 기준: 핵심 상수 및 판례 논리]
1. 적정 이자율: 연 4.6% (상속세 및 증여세법 시행규칙 제10조의5, 법인세법 시행규칙 제43조 제2항 — 1,000분의 46)
2. 증여세 과세 기준점(Threshold): 이자 이익이 "연간 1,000만 원 이상"일 때만 과세.
   - 공식: (대출금액 × 4.6%) - (실제 지급한 이자)
   - 이자 이익이 1,000만 원 미만인 경우, 증여세가 과세되지 않으며 다른 증여 재산과 합산되지 않음 (서면-2018-상속증여-2137).
   - 여러 명에게 빌릴 경우, 각 대출자별로 인당 개별 계산해야 함.
3. 특수관계인 범위:
   - 직계존비속, 배우자: 무조건 특수관계인
   - 사돈 (직계비속의 배우자의 2촌 이내 부계혈족과 그 배우자): 특수관계인에 포함됨 (재산세과-230).
4. 사실 판단 5가지 요건 (차용증 인정 요건):
   - (1) 계약서(공정증서 등) 작성 여부
   - (2) 이자 지급 사실 (계좌이체 내역 등 객관적 증빙)
   - (3) 차입 및 원금 상환 내역
   - (4) 대여자의 자금 출처
   - (5) 차입자의 자금 사용처

### [분석 프로세스]
1. 관계 추출: 질문에서 돈을 빌려주는 사람과 빌리는 사람의 관계를 파악하고, [특수관계인] 여부를 판정하라.
2. 금액 분석: 대출 금액과 명시된 이자율(없으면 0%)을 파악하라.
3. 이익 계산: "(대출금액 × 4.6%) - (실제 이자)" 공식을 계산하여, 이익이 1,000만 원을 넘는지 확인하라.
4. 결과 안내: 과세/비과세 여부를 명확한 근거(숫자 계산식)와 함께 제시하고, [사실 판단 요건]을 경고로 덧붙여라.

### [답변 형식]
- 👨‍👩‍👧‍👦 **관계 분석**: (특수관계인 여부 판정)
- 🧮 **증여 이익 계산**: (공식을 통한 계산 과정 명시)
- ⚖️ **세무적 판단**: (1,000만 원 임계값 기준 결과)
- 🚨 **핵심 주의사항**: (차용증 작성 및 이자 지급 시 주의점)

### [중요 지시]
- 아래 [RAG 검색 결과]에 관련 법령이나 판례가 있으면 반드시 참조하여 답변하세요.
- 답변은 한국어로 작성하세요.
- 계산이 필요한 경우 반드시 숫자와 공식을 명시하세요."""

class ChatbotService:
    def __init__(self):
        self.llm_client = GMSGeminiClient()
        self.rag_engine = get_rag_engine()
        
    async def get_chat_response(self, history: list, message: str) -> str:
        """
        [기능 3] 세법 및 법률 상담 챗봇 파이프라인 (시맨틱 캐시 통합)
        """
        query_vector = None
        try:
            # RAG 엔진의 임베딩 모델을 사용하여 질문 벡터화
            query_vector = self.rag_engine.embedding_model.embed_query(message)
            cached_reply = await cache_manager.get_cache(query_vector)
            
            if cached_reply:
                return cached_reply
        except Exception as e:
            logger.warning(f"⚠️ 시맨틱 캐시 조회 중 오류 (무시하고 진행): {str(e)}")

        # 1. RAG 검색 (k=4)
        rag_context = self.rag_engine.search_similar_documents(message, k=4)
        
        # 2. 메시지 보강
        enhanced_message = (
            f"[사용자 질의]\n{message}\n\n"
            f"[RAG 검색된 관련 법률/판례 근거]\n{rag_context}"
        )
        
        # 3. LLM 호출
        ai_reply = await self.llm_client.generate_chat(
            history, 
            enhanced_message, 
            system_instruction=LEGAL_SYSTEM_PROMPT
        )
        
        # 4. 결과 캐시에 저장
        if query_vector:
            try:
                await cache_manager.set_cache(query_vector, message, ai_reply)
            except Exception as e:
                logger.warning(f"⚠️ 시맨틱 캐시 저장 중 오류: {str(e)}")
            
        return ai_reply

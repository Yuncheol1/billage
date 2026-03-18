from app.services.gms_engine import GMSGeminiClient
from app.services.rag_engine import get_rag_engine
import json

class AnalyzeService:
    def __init__(self):
        self.llm_client = GMSGeminiClient()
        self.rag_engine = get_rag_engine()
        
    async def analyze_document(self, text: str, mode: str, principal: int = None, interest: float = None) -> dict:
        """
        [기능 1] 파이프라인
        1. RAG 검색 (법률 지식 추출 부분 - 현재는 Mock 또는 생략, 추후 Chroma 연동)
        2. LLM에 컨텍스트 전달하여 독소조항 및 세금 이슈 분석
        """
        
        # 임베딩 비용/시간 최적화: 검색을 위한 질의 조합
        search_query = f"차용증 계약서 내용 분석 중단: {text}"
        if principal and interest is not None:
             search_query += f" 원금 {principal}원에 무이자 또는 금리 {interest}% 대여금 증여세 문제, 이자제한법 최고이자율"
        
        # RAG 검색 실행
        rag_context = self.rag_engine.search_similar_documents(search_query, k=3)
        
        # 만약 DB가 비어있어서 검색 결과가 없다면, Fallback Context를 제공
        if "찾을" in rag_context or "오류" in rag_context:
             rag_context = """
             [관련 기본 법령 파싱됨 (DB 미설정에 의한 Fallback)]
             - 이자제한법 최고이자율: 연 20%
             - 무이자 또는 적정 이자율(연 4.6%)보다 낮은 이자율로 금전을 대여한 경우 상속세 및 증여세법에 따른 증여세 대상이 될 수 있음
             - 민법 제103조: 반사회적 법률행위(신체 포기 등)는 무효로 함
             """
        
        system_prompt = f"""
        당신은 전문 변호사 및 세무사입니다.
        주어진 [차용증 내용]을 검토하여, [관련 법령/판례 컨텍스트]를 바탕으로 독소조항(법적 무효 위험)과 세금 이슈(증여세 등)를 분석하세요.
        리스크가 있다면 HIGH, MEDIUM, LOW로 분류하고 사유와 대안을 제시하세요.
        다음 JSON 형식으로만 정확히 응답해야 합니다. Markdown 백틱(```json)을 포함하지 마세요.
        {{
            "is_safe": false,
            "summary": "총평",
            "warnings": [
                {{
                    "risk_level": "HIGH",
                    "category": "TOXIC_CLAUSE 또는 TAX_ISSUE",
                    "reason": "사유 설명",
                    "suggestion": "수정 권고안"
                }}
            ]
        }}
        """
        
        user_prompt = f"""
        {rag_context}
        
        [차용증 내용]
        {text}
        
        (추가정보: 원금 {principal}원, 이자 {interest}%)
        """
        
        ai_response = await self.llm_client.generate_response(system_instruction=system_prompt, user_prompt=user_prompt)
        
        try:
            # Markdown 백틱이 섞여있을 수 있으므로 제거
            clean_json = ai_response.replace('```json', '').replace('```', '').strip()
            return json.loads(clean_json)
        except json.JSONDecodeError:
            # 파싱 실패 시 기본 폼 리턴
            return {
                "is_safe": False,
                "summary": "AI 응답 파싱 실패",
                "warnings": []
            }

from app.services.gms_engine import GMSGeminiClient
import json

class ClauseMakerService:
    def __init__(self):
        self.llm_client = GMSGeminiClient()
        
    async def generate_legal_clause(self, user_intent: str, context: str = "") -> dict:
        """
        [기능 2] 자연어 -> 특약사항 문구 변환 파이프라인
        """
        system_prompt = """
        당신은 대한민국 변호사입니다.
        사용자가 일상 용어나 자연어로 입력한 계약 조건(의도)을 법적으로 유효하고 명확한 '특약사항' 문장으로 변환해야 합니다.
        너무 길지 않게 핵심만 간결하게 작성하며, 무효가 될 수 있는 반사회적 내용은 적법한 범위 내에서 순화해서 제안하세요.
        응답은 다음 JSON 형식으로만 정확히 반환하세요. Markdown 백틱(```json)을 포함하지 마세요.
        {
            "legal_clause": "법률 용어로 정제된 특약사항 한 줄",
            "explanation": "해당 조항의 법적 의미 및 효력에 대한 설명 (1~2줄)"
        }
        """
        
        user_prompt = f"""
        [작성 중인 차용증 문맥]
        {context if context else "없음"}
        
        [사용자 의도(요청사항)]
        {user_intent}
        """
        
        ai_response = await self.llm_client.generate_response(system_instruction=system_prompt, user_prompt=user_prompt)
        
        try:
            clean_json = ai_response.replace('```json', '').replace('```', '').strip()
            return json.loads(clean_json)
        except json.JSONDecodeError:
            return {
                "legal_clause": "AI 변환 실패. 다시 시도해 주세요.",
                "explanation": ""
            }

from app.services.gms_engine import GMSGeminiClient
import json

class ClauseMakerService:
    def __init__(self):
        self.llm_client = GMSGeminiClient()
        
    async def generate_legal_clause(self, user_intent: str, context: str = "", template_type: str = None) -> dict:
        """
        [기능 2] 자연어 → 특약사항 문구 변환 파이프라인
        응답은 contract_clause 테이블에 바로 INSERT 가능한 구조(clause_id, title, content)
        """
        # 계약 유형에 따른 맥락 보강
        type_context = ""
        if template_type:
            type_map = {
                "FAMILY_JEONSE": "가족 간 전세 계약",
                "FRIEND_SMALL": "지인 간 소액 대여 계약",
                "PARTNER_BIZ": "사업 동업자 간 대여 계약",
                "COLLEAGUE_EMERGENCY": "직장 동료 간 긴급 대여 계약"
            }
            type_context = f"\n계약 유형: {type_map.get(template_type, template_type)}"
        
        system_prompt = f"""
        당신은 대한민국 변호사입니다.
        사용자가 자연어로 입력한 계약 조건(의도)을 법적으로 유효한 '특약사항' 문장으로 변환하세요.
        너무 길지 않게 핵심만 간결하게 작성하며, 반사회적 내용은 적법 범위 내에서 순화하세요.
        
        ### 거부 조건 (반드시 준수)
        아래에 해당하는 입력은 특약사항으로 변환하지 말고, 거부 응답을 반환하세요:
        1. 차용증·계약·금전 거래와 전혀 무관한 내용 (일상 대화, 장난, 자기소개 등)
        2. 의미를 알 수 없는 문장 (욕설, 무작위 문자열, 비속어 등)
        3. 구체적인 계약 조건이 전혀 포함되지 않은 입력
        
        거부 시 아래 JSON을 반환하세요:
        {{
            "rejected": true,
            "rejection_reason": "거부 사유를 사용자가 이해할 수 있게 설명"
        }}
        
        정상 변환 시 아래 JSON 형식으로 반환하세요. Markdown 백틱을 포함하지 마세요.
        {{
            "rejected": false,
            "clause_id": "조항 유형 코드 (PENALTY/COLLATERAL/EARLY_REPAYMENT/GUARANTEE/OTHER 중 택 1)",
            "title": "특약 조항 제목 (예: 차량 양도담보 설정)",
            "legal_clause": "법률 용어로 정제된 특약사항 본문",
            "explanation": "해당 조항의 법적 의미 및 효력에 대한 설명 (1~2줄)"
        }}
        """
        
        user_prompt = f"""
        [작성 중인 차용증 문맥]{type_context}
        {context if context else "없음"}
        
        [사용자 의도(요청사항)]
        {user_intent}
        """
        
        ai_response = await self.llm_client.generate_response(
            system_instruction=system_prompt, 
            user_prompt=user_prompt
        )
        
        try:
            clean_json = ai_response.replace('```json', '').replace('```', '').strip()
            result = json.loads(clean_json)
            # rejected 필드가 없으면 기본값 False 보장
            if "rejected" not in result:
                result["rejected"] = False
            return result
        except json.JSONDecodeError:
            return {
                "rejected": False,
                "clause_id": "OTHER",
                "title": "특약사항",
                "legal_clause": "AI 변환 실패. 다시 시도해 주세요.",
                "explanation": ""
            }

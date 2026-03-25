from app.services.gms_engine import GMSGeminiClient
from app.services.rag_engine import get_rag_engine
from app.schemas.analyze import AnalyzeRequest
import json

class AnalyzeService:
    def __init__(self):
        self.llm_client = GMSGeminiClient()
        self.rag_engine = get_rag_engine()
        
    async def analyze_document(self, request: AnalyzeRequest) -> dict:
        """
        [기능 1] ERD 기반 파이프라인
        1. Spring이 전달한 contract 메타데이터 + contract_clause 리스트를 활용
        2. RAG 검색으로 관련 법령/판례 컨텍스트 확보
        3. Gemini에 전달하여 독소조항/세금 이슈/tax_summary 분석
        """
        
        # --- 1. RAG 검색 쿼리 조합 (ERD 메타데이터 활용) ---
        search_query = f"차용증 분석: {request.contract_text}"
        
        # contract 테이블의 정형 데이터로 검색 정확도 향상
        if request.template_type:
            type_map = {
                "FAMILY_JEONSE": "가족 간 전세 대여",
                "FRIEND_SMALL": "지인 간 소액 대여",
                "PARTNER_BIZ": "사업 동업자 간 대여",
                "COLLEAGUE_EMERGENCY": "직장 동료 간 긴급 대여"
            }
            search_query += f" {type_map.get(request.template_type, request.template_type)}"
        
        if request.interest_rate is not None:
            # 소수 표기(0.046) → 퍼센트(4.6%) 변환
            rate_pct = request.interest_rate * 100 if request.interest_rate < 1 else request.interest_rate
            search_query += f" 이자율 {rate_pct}% 증여세 이자제한법"
        
        if request.principal_amount:
            search_query += f" 원금 {int(request.principal_amount)}원"
        
        # RAG 검색 실행
        rag_context = self.rag_engine.search_similar_documents(search_query, k=3)
        
        # Fallback: DB가 비어있을 경우
        if "찾을" in rag_context or "오류" in rag_context:
            rag_context = """
            [관련 기본 법령 (Fallback)]
            - 이자제한법 최고이자율: 연 20%
            - 적정이자율 연 4.6% 미만의 무이자/저이자 대여 → 증여세 대상 (상속세 및 증여세법 제41조의4)
            - 민법 제103조: 반사회적 법률행위(신체 포기 등)는 무효
            - 이자소득세 원천징수 세율: 27.5% (지방세 포함)
            """
        
        # --- 2. 개별 조항(clauses) 텍스트 조합 ---
        clauses_text = ""
        if request.clauses:
            for c in request.clauses:
                clause_label = f"[{c.clause_id}]" if c.clause_id else ""
                clauses_text += f"\n- {clause_label} {c.title or ''}: {c.content}"
        
        # --- 3. 이자율 퍼센트 변환 ---
        rate_display = "없음(무이자)"
        if request.interest_rate is not None:
            rate_pct = request.interest_rate * 100 if request.interest_rate < 1 else request.interest_rate
            rate_display = f"{rate_pct}%"
        
        # --- 4. LLM 프롬프트 구성 ---
        system_prompt = f"""
        당신은 대한민국 전문 변호사 및 세무사입니다.
        주어진 [차용증 정보]를 검토하여, [관련 법령/판례]를 바탕으로 독소조항과 세금 이슈를 분석하세요.
        
        분석 시 반드시 고려할 사항:
        - template_type이 FAMILY인 경우, 가족 간 거래이므로 증여세 추징 위험을 특히 중점 분석
        - interest_rate가 0.046(4.6%) 미만이면 증여세 위험 경고 필수
        - interest_rate가 0.20(20%)을 초과하면 이자제한법 위반 경고 필수
        - 신체 포기, 인격권 침해 등 반사회적 조항은 민법 제103조에 의거 무효 경고
        
        [review_result 분류 기준] — 반드시 아래 4개 값 중 하나로 설정:
        - "SAFE": 위험 항목이 전혀 없는 안전한 계약
        - "TAX_RISK_ONLY": 증여세·상속세·원천징수 등 세무 이슈만 존재 (계약 자체는 법적으로 유효하므로 서명 가능)
        - "LEGAL_VIOLATION": 이자제한법 위반, 반사회적 조항(신체포기 등), 무효 조항 등 법적 위반만 존재 (서명 불가)
        - "BOTH": 세무 이슈와 법적 위반이 모두 존재 (서명 불가)
        
        다음 JSON 형식으로만 정확히 응답하세요. Markdown 백틱을 포함하지 마세요.
        {{
            "is_safe": false,
            "review_result": "SAFE/TAX_RISK_ONLY/LEGAL_VIOLATION/BOTH",
            "summary": "총평",
            "warnings": [
                {{
                    "risk_level": "HIGH/MEDIUM/LOW",
                    "category": "TOXIC_CLAUSE/TAX_ISSUE/MISSING_INFO",
                    "clause_id": "문제가 된 조항의 clause_id (해당 없으면 null)",
                    "reason": "사유 설명",
                    "suggestion": "수정 권고안"
                }}
            ],
            "tax_summary": {{
                "gift_tax_risk": true,
                "estimated_gift_tax": 460000,
                "withholding_tax_rate": "27.5%"
            }}
        }}
        """
        
        user_prompt = f"""
        {rag_context}
        
        [차용증 정보]
        - 계약 유형: {request.template_type or "미지정"}
        - 원금: {int(request.principal_amount) if request.principal_amount else "미지정"}원
        - 이자율: {rate_display}
        - 상환 방식: {request.repayment_method or "미지정"}
        - 대출 기간: {request.start_dt or "미지정"} ~ {request.end_dt or "미지정"}
        
        [차용증 본문]
        {request.contract_text}
        
        [개별 조항]
        {clauses_text if clauses_text else "없음"}
        """
        
        ai_response = await self.llm_client.generate_response(
            system_instruction=system_prompt, 
            user_prompt=user_prompt
        )
        
        try:
            clean_json = ai_response.replace('```json', '').replace('```', '').strip()
            return json.loads(clean_json)
        except json.JSONDecodeError:
            return {
                "is_safe": False,
                "summary": "AI 응답 파싱 실패",
                "warnings": [],
                "tax_summary": None
            }

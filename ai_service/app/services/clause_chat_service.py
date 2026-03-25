import json
import logging
from typing import List, Optional
from app.services.gms_engine import GMSGeminiClient
from app.services.rag_engine import get_rag_engine

logger = logging.getLogger(__name__)


class ClauseChatService:
    """
    두 사용자 간 채팅 내역을 분석하여 법적 특약사항을 자동 생성하는 서비스.
    Spring이 MongoDB에서 조회·가공한 채팅 메시지 배열을 입력으로 받습니다.
    """

    def __init__(self):
        self.llm_client = GMSGeminiClient()
        self.rag_engine = get_rag_engine()

    async def generate_clauses_from_chat(
        self,
        chat_messages: list,
        contract_context: str = "",
        template_type: Optional[str] = None,
    ) -> dict:
        """
        채팅 메시지 배열을 분석하여 복수의 특약사항을 생성합니다.

        Returns:
            {"clauses": [...], "summary": "..."}
        """
        # 1. 채팅 내역을 대화 텍스트로 변환
        conversation_text = self._format_conversation(chat_messages)

        # 2. 계약 유형 맥락 보강
        type_context = ""
        if template_type:
            type_map = {
                "FAMILY_JEONSE": "가족 간 전세 계약",
                "FRIEND_SMALL": "지인 간 소액 대여 계약",
                "PARTNER_BIZ": "사업 동업자 간 대여 계약",
                "COLLEAGUE_EMERGENCY": "직장 동료 간 긴급 대여 계약",
            }
            type_context = f"\n계약 유형: {type_map.get(template_type, template_type)}"

        # 3. RAG 검색으로 관련 법률/판례 컨텍스트 확보
        # 대화 전체를 쿼리로 사용하면 토큰 낭비 → 핵심 키워드 추출용 요약 쿼리 사용
        rag_query = f"차용증 특약사항 {conversation_text[:200]}"
        rag_context = self.rag_engine.search_similar_documents(rag_query, k=3)

        # 4. 시스템 프롬프트: 대화 분석 → 복수 특약 생성
        system_prompt = """당신은 대한민국 변호사이자 차용증 작성 전문가입니다.
두 사용자(채권자/채무자) 간의 채팅 대화 내역을 분석하여,
대화에서 합의되거나 요구된 조건을 법적으로 유효한 '특약사항'으로 변환하세요.

### 핵심 규칙
1. 대화에 나타난 모든 계약 관련 합의/요구 사항을 빠짐없이 추출하세요.
2. 반사회적이거나 불법적인 내용은 적법한 범위 내에서 순화하세요.
3. 일상적인 인사나 계약과 무관한 잡담은 무시하세요.
4. 대화에 특약으로 만들 내용이 없으면 빈 배열을 반환하세요.

### 응답 형식 (반드시 아래 JSON만 반환하세요. Markdown 백틱 금지.)
{
    "clauses": [
        {
            "clause_id": "조항 유형 코드 (PENALTY/COLLATERAL/EARLY_REPAYMENT/INTEREST/GUARANTEE/OTHER 중 택 1)",
            "title": "특약 조항 제목",
            "legal_clause": "법률 용어로 정제된 특약사항 본문",
            "explanation": "해당 조항의 법적 의미 및 효력 설명 (1~2줄)"
        }
    ],
    "summary": "대화 분석 요약 (어떤 합의 의도를 파악했는지 1~2줄)"
}"""

        # 5. 사용자 프롬프트: 채팅 내역 + 문맥 전달
        user_prompt = f"""[작성 중인 차용증 문맥]{type_context}
{contract_context if contract_context else "없음"}

[관련 법률/판례 참고자료]
{rag_context}

[두 사용자 간 채팅 대화 내역]
{conversation_text}"""

        # 6. LLM 호출
        ai_response = await self.llm_client.generate_response(
            system_instruction=system_prompt,
            user_prompt=user_prompt,
        )

        # 7. JSON 파싱
        return self._parse_response(ai_response)

    def _format_conversation(self, chat_messages: list) -> str:
        """채팅 메시지 배열을 가독성 있는 대화 텍스트로 변환"""
        lines = []
        for msg in chat_messages:
            sender = f"사용자{msg.get('sender_seq', '?')}"
            content = msg.get("content", "")
            msg_type = msg.get("msg_type", "TEXT")

            # AI_PROPOSAL 등 비텍스트 메시지는 내용만 추출 시도
            if msg_type != "TEXT":
                try:
                    parsed = json.loads(content)
                    # 계약 제안 메시지인 경우 핵심 정보만 추출
                    content = f"[계약 제안] {parsed.get('contractTitle', '')} - {', '.join(parsed.get('modifications', []))}"
                except (json.JSONDecodeError, AttributeError):
                    pass  # 파싱 실패 시 원본 content 그대로 사용

            lines.append(f"{sender}: {content}")

        return "\n".join(lines)

    def _parse_response(self, ai_response: str) -> dict:
        """LLM 응답을 JSON으로 파싱. 실패 시 안전한 기본값 반환"""
        try:
            clean_json = ai_response.replace("```json", "").replace("```", "").strip()
            result = json.loads(clean_json)

            # clauses 배열이 없으면 빈 배열 보장
            if "clauses" not in result:
                result["clauses"] = []
            if "summary" not in result:
                result["summary"] = ""

            return result
        except json.JSONDecodeError:
            logger.warning(f"⚠️ AI 응답 JSON 파싱 실패. 원본: {ai_response[:300]}")
            return {
                "clauses": [],
                "summary": "AI 응답 파싱에 실패했습니다. 다시 시도해 주세요.",
            }

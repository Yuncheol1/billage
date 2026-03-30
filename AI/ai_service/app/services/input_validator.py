"""
비정상 입력 사전 필터링 모듈 (1계층 방어)
LLM 호출 전에 명백히 비정상적인 입력을 저비용으로 거부합니다.
"""
import re
import logging
from typing import Tuple, Optional, List
from app.core.config import settings

logger = logging.getLogger(__name__)

# 비속어/욕설 키워드 목록 (필요 시 확장)
_PROFANITY_KEYWORDS = [
    "시발", "씨발", "ㅅㅂ", "ㅆㅂ", "병신", "ㅂㅅ", "지랄", "ㅈㄹ",
    "개새끼", "새끼", "ㅅㄲ", "미친", "꺼져", "닥쳐",
    "똥", "씹", "ㅆ", "좆", "ㅈ같",
]

# 최소 입력 길이 (한국어 기준, 의미 있는 문장의 최소 길이)
_MIN_INTENT_LENGTH = 4


def validate_user_intent(text: str) -> Tuple[bool, Optional[str]]:
    """
    단일 자연어 입력(user_intent)의 유효성을 검증합니다.

    Returns:
        (is_valid, rejection_reason)
        - is_valid=True: 정상 → LLM 호출 진행
        - is_valid=False: 비정상 → rejection_reason과 함께 즉시 거부
    """
    if not text or not text.strip():
        return False, "입력이 비어 있습니다. 원하시는 계약 조건을 입력해 주세요."

    cleaned = text.strip()

    if len(cleaned) > settings.MAX_USER_INTENT_LENGTH:
        return False, f"입력이 너무 깁니다 (최대 {settings.MAX_USER_INTENT_LENGTH}자). 핵심 계약 조건만 입력해 주세요."

    # 1. 최소 길이 검사
    if len(cleaned) < _MIN_INTENT_LENGTH:
        return False, f"입력이 너무 짧습니다 (최소 {_MIN_INTENT_LENGTH}자). 구체적인 계약 조건을 입력해 주세요."

    # 2. 동일 문자 반복 감지 (예: "ㅋㅋㅋㅋㅋ", "ㅎㅎㅎㅎ", "aaaa")
    if _is_repetitive(cleaned):
        return False, "의미 있는 계약 조건을 입력해 주세요. (반복 문자 감지)"

    # 3. 비속어/욕설 필터링
    profanity_found = _check_profanity(cleaned)
    if profanity_found:
        return False, "부적절한 표현이 포함되어 있습니다. 계약 조건을 정상적으로 입력해 주세요."

    # 4. 한국어/영문/숫자 비율 검사 (특수문자 도배 방지)
    if not _has_meaningful_content(cleaned):
        return False, "의미 있는 텍스트를 입력해 주세요. (특수문자 또는 무작위 문자열 감지)"

    return True, None


def validate_chat_messages(messages: List[dict]) -> Tuple[bool, Optional[str]]:
    """
    채팅 메시지 배열의 유효성을 검증합니다.

    Returns:
        (is_valid, rejection_reason)
    """
    if not messages:
        return False, "채팅 메시지가 비어 있습니다."

    if len(messages) > settings.MAX_CHAT_MESSAGES:
        return False, f"채팅 메시지가 너무 많습니다 (최대 {settings.MAX_CHAT_MESSAGES}개). 최근 대화 위주로 전달해 주세요."

    # 의미 있는 내용이 있는 메시지가 하나라도 있는지 확인
    meaningful_count = 0
    for msg in messages:
        content = msg.get("content", "").strip()
        if len(content) > settings.MAX_CHAT_MESSAGE_LENGTH:
            return False, f"채팅 메시지 하나가 너무 깁니다 (최대 {settings.MAX_CHAT_MESSAGE_LENGTH}자)."
        if not content:
            continue
        if _is_repetitive(content):
            return False, "반복 문자가 많은 채팅은 처리할 수 없습니다. 계약 관련 대화만 전달해 주세요."
        if not _has_meaningful_content(content):
            continue
        if _check_profanity(content):
            return False, "부적절한 표현이 포함된 채팅은 처리할 수 없습니다."
        if len(content) >= 2:
            meaningful_count += 1

    if meaningful_count == 0:
        return False, "의미 있는 채팅 내용이 없습니다. 계약 관련 대화를 입력해 주세요."

    return True, None


def _is_repetitive(text: str) -> bool:
    """동일 문자가 전체 길이의 70% 이상을 차지하면 반복으로 판단"""
    if len(text) < 3:
        return False

    # 각 문자의 빈도 계산
    char_counts = {}
    for ch in text.replace(" ", ""):
        char_counts[ch] = char_counts.get(ch, 0) + 1

    text_len_no_space = len(text.replace(" ", ""))
    if text_len_no_space == 0:
        return True

    max_ratio = max(char_counts.values()) / text_len_no_space
    return max_ratio >= 0.7


def _check_profanity(text: str) -> bool:
    """비속어 키워드가 포함되어 있는지 검사"""
    lower_text = text.lower()
    for keyword in _PROFANITY_KEYWORDS:
        if keyword in lower_text:
            logger.info(f"⚠️ 비속어 감지: '{keyword}' in input")
            return True
    return False


def _has_meaningful_content(text: str) -> bool:
    """
    한국어, 영문, 숫자가 전체의 40% 이상인지 확인.
    특수문자 도배나 무작위 기호 입력을 방지합니다.
    """
    # 한국어(가-힣), 영문(a-zA-Z), 숫자(0-9)
    meaningful_chars = len(re.findall(r'[가-힣a-zA-Z0-9]', text))
    total_chars = len(text.replace(" ", ""))

    if total_chars == 0:
        return False

    return (meaningful_chars / total_chars) >= 0.4

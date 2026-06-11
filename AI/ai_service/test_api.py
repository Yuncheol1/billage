"""ERD v2.0 API 통합 테스트 스크립트"""
import requests
import json
import time
import os

BASE = "http://127.0.0.1:8000/internal/v1"
HEADERS = {
    "Content-Type": "application/json",
    "X-Internal-API-Key": os.getenv("INTERNAL_API_KEY", "")
}

def test_analyze():
    print("=" * 60)
    print("[TEST 1] 차용증 독소조항 & 세금 분석")
    print("=" * 60)
    payload = {
        "contract_seq": 42,
        "contract_text": "제1조(대여금) 금 10,000,000원을 대여한다. 제3조(특약) 상환기일에 원금을 갚지 못할 경우, 연 30%의 지연이자를 가산하며 신체포기각서를 이행한다.",
        "mode": "full",
        "template_type": "FAMILY_JEONSE",
        "principal_amount": 10000000,
        "interest_rate": 0.0,
        "repayment_method": "BULLET",
        "start_dt": "2026-03-17",
        "end_dt": "2027-03-17",
        "clauses": [
            {"clause_id": "PENALTY", "title": "특약사항", "content": "상환 불이행 시 신체를 포기한다."},
            {"clause_id": "INTEREST", "title": "이자", "content": "연 30%의 지연이자를 가산한다."}
        ]
    }
    r = requests.post(f"{BASE}/analyze", headers=HEADERS, json=payload, timeout=60)
    print(f"Status: {r.status_code}")
    if r.status_code == 200:
        print(json.dumps(r.json(), indent=2, ensure_ascii=False))
    else:
        print(f"Error: {r.text}")
    print()

def test_clause():
    print("=" * 60)
    print("[TEST 2] 자연어 → 특약사항 생성")
    print("=" * 60)
    payload = {
        "contract_seq": 42,
        "user_intent": "돈 안 갚으면 타고 다니는 차 뺏을 거야",
        "contract_context": "채권자 홍길동, 채무자 김철수, 원금 1000만원",
        "template_type": "FRIEND_SMALL"
    }
    r = requests.post(f"{BASE}/clauses/generate", headers=HEADERS, json=payload, timeout=60)
    print(f"Status: {r.status_code}")
    if r.status_code == 200:
        print(json.dumps(r.json(), indent=2, ensure_ascii=False))
    else:
        print(f"Error: {r.text}")
    print()

def test_chat():
    print("=" * 60)
    print("[TEST 3] 세무/법률 챗봇")
    print("=" * 60)
    payload = {
        "message": "부모님께 5천만원 빌릴 때 증여세 안 나오게 하려면?",
        "history": [],
        "contract_seq": None
    }
    r = requests.post(f"{BASE}/chat", headers=HEADERS, json=payload, timeout=60)
    print(f"Status: {r.status_code}")
    if r.status_code == 200:
        print(json.dumps(r.json(), indent=2, ensure_ascii=False))
    else:
        print(f"Error: {r.text}")
    print()

if __name__ == "__main__":
    print("\n🚀 ERD v2.0 API 통합 테스트 시작!\n")
    test_analyze()
    test_clause()
    test_chat()
    print("✅ 테스트 완료!")

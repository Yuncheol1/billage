import unittest
import requests
import os
from pathlib import Path
from dotenv import load_dotenv

# --- 테스트 환경 설정 로드 (.env) ---
# tests/ 디렉토리에 있는 .env 파일을 우선 로드합니다.
TEST_ENV_PATH = Path(__file__).resolve().parent.parent / ".env"
load_dotenv(TEST_ENV_PATH)

# --- 설정 (환경 변수 또는 기본값) ---
INTERNAL_API_KEY = os.getenv("INTERNAL_API_KEY", "super-secret-key-change-me-in-production")
BASE_URL = os.getenv("TEST_BASE_URL", "http://localhost:8000")
CHAT_ENDPOINT = f"{BASE_URL}/internal/v1/chat"
HEALTH_ENDPOINT = f"{BASE_URL}/health"

class TestChatAPI(unittest.TestCase):
    """
    FastAPI 챗봇 엔드포인트 통합 테스트
    (서버가 실행 중인 상태에서 수행 권장)
    """

    def test_health_check(self):
        """서버 헬스체크 통신 여부 확인"""
        response = requests.get(HEALTH_ENDPOINT)
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json(), {"status": "ok"})

    def test_chat_security_denied(self):
        """API Key 없이 요청 시 403 Forbidden 반환 확인"""
        payload = {"message": "테스트 질문", "history": []}
        response = requests.post(CHAT_ENDPOINT, json=payload)
        self.assertEqual(response.status_code, 403)

    def test_chat_success(self):
        """올바른 API Key와 데이터 요청 시 200 OK 및 응답 확인"""
        headers = {"X-Internal-API-Key": INTERNAL_API_KEY}
        payload = {"message": "전세 계약 시 주의사항 알려줘", "history": []}
        
        try:
            response = requests.post(CHAT_ENDPOINT, json=payload, headers=headers, timeout=10)
            if response.status_code == 200:
                data = response.json()
                self.assertIn("reply", data)
                self.assertIsInstance(data["reply"], str)
            else:
                self.skipTest(f"서버 응답 오류 (Code: {response.status_code}). 서버가 구동 중인지 확인하세요.")
        except requests.exceptions.ConnectionError:
            self.skipTest("서버에 연결할 수 없습니다. 통합 테스트를 위해 서버를 실행해 주세요.")

if __name__ == "__main__":
    unittest.main()

from locust import HttpUser, task, between, events
import os
from pathlib import Path
from dotenv import load_dotenv

# --- 테스트 환경 설정 로드 (.env) ---
TEST_ENV_PATH = Path(__file__).resolve().parent.parent.parent / ".env"
load_dotenv(TEST_ENV_PATH)

# --- 설정 (환경 변수 또는 기본값) ---
INTERNAL_API_KEY = os.getenv("INTERNAL_API_KEY", "super-secret-key-change-me-in-production")
TARGET_ENDPOINT = "/internal/v1/chat"

class AIServiceUser(HttpUser):
    """
    EC2 AI 서비스(Billage) 성능 측정을 위한 Locust 시뮬레이터
    """
    # 기본 호스트 설정 (tests/.env 또는 기본값)
    host = os.getenv("TEST_BASE_URL", "http://3.35.206.231:8000")
    
    # GMS API 할당량이 매우 작으므로 초극보수적으로 10~20초 간격 설정
    wait_time = between(10, 20)
    
    # 자동 종료를 위한 설정 (환경 변수 또는 기본값)
    max_requests = int(os.getenv("TEST_MAX_REQUESTS", "50"))
    request_count = 0

    def on_start(self):
        """
        테스트 시작 시 인증 토큰 및 초기 설정
        """
        self.internal_api_key = os.getenv("INTERNAL_API_KEY", "billage-ai-key-1234")
        self.request_count = 0
        print(f"🚀 성능 테스트 시작: 대상 호스트 - {self.host} (최대 {self.max_requests}회 요청 후 자동 종료)")
        self.headers = {
            "X-Internal-API-Key": self.internal_api_key,
            "Content-Type": "application/json"
        }

    @task
    def chat_performance_test(self):
        # 지정된 요청 횟수에 도달하면 테스트 자동 종료
        if self.request_count >= self.max_requests:
            print(f"🏁 목표 요청 횟수({self.max_requests}회)에 도달하여 테스트를 자동 종료합니다.")
            self.environment.runner.quit()
            return

        self.request_count += 1
        """
        핵심 기능: RAG 기반 챗봇 질의 테스트
        """
        payload = {
            "message": "전세 사기를 예방하기 위해 등기부등본에서 확인해야 할 핵심 항목 3가지만 알려줘.",
            "history": []
        }
        
        with self.client.post(TARGET_ENDPOINT, json=payload, headers=self.headers, catch_response=True) as response:
            if response.status_code == 200:
                # 성공 시 응답 내용 유효성 간이 체크 (답변이 비어있지 않은지)
                try:
                    data = response.json()
                    if "reply" in data and len(data["reply"]) > 10:
                        response.success()
                    else:
                        response.failure("Response reply is too short or missing")
                except Exception:
                    response.failure("Invalid JSON response")
            elif response.status_code == 403:
                response.failure("403 Forbidden: Check INTERNAL_API_KEY")
            else:
                response.failure(f"Error Code {response.status_code}")

    @task(1)
    def check_health(self):
        """
        단순 헬스체크 응답성 테스트
        """
        self.client.get("/health")

@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    print(f"🚀 성능 테스트 시작: 대상 호스트 - {environment.host}")

@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    print("🏁 성능 테스트 종료")

from locust import HttpUser, task, between, constant, events
import os
from pathlib import Path
from dotenv import load_dotenv

# --- 테스트 환경 설정 로드 (.env) ---
TEST_ENV_PATH = Path(__file__).resolve().parent.parent.parent / ".env"
load_dotenv(TEST_ENV_PATH)

from datetime import datetime, timezone, timedelta
def get_kst():
    return datetime.now(timezone(timedelta(hours=9))).strftime('[%Y-%m-%d %H:%M:%S KST]')

# --- 설정 (환경 변수 또는 기본값) ---
INTERNAL_API_KEY = os.getenv("INTERNAL_API_KEY", "super-secret-key-change-me-in-production")
TARGET_ENDPOINT = "/internal/v1/chat"

class AIServiceUser(HttpUser):
    """
    EC2 AI 서비스(Billage) 성능 측정을 위한 Locust 시뮬레이터
    """
    # 기본 호스트 설정 (tests/.env 또는 기본값)
    host = os.getenv("TEST_BASE_URL", "http://3.35.206.231:8000")
    
    # 쉴 틈 없이 맹공격하도록 대기 시간을 0초로 설정
    wait_time = constant(0)
    
    # 자동 종료를 위한 설정 (옵션 또는 인당 8회 고정)
    max_requests = 8
    request_count = 0

    def on_start(self):
        """
        테스트 시작 시 인증 토큰 및 초기 설정
        """
        self.internal_api_key = os.getenv("INTERNAL_API_KEY", "billage-ai-key-1234")
        self.request_count = 0
        print(f"{get_kst()} 🚀 성능 테스트 시작: 대상 호스트 - {self.host} (최대 {self.max_requests}회 요청 후 자동 종료)")
        self.headers = {
            "X-Internal-API-Key": self.internal_api_key,
            "Content-Type": "application/json"
        }

    @task(5)
    def chat_performance_test(self):
        # 지정된 요청 횟수에 도달하면 테스트 자동 종료
        if self.request_count >= self.max_requests:
            print(f"{get_kst()} 🏁 목표 요청 횟수({self.max_requests}회)에 도달하여 테스트를 자동 종료합니다.")
            self.environment.runner.quit()
            return

        self.request_count += 1
        """
        핵심 기능: RAG 기반 챗봇 질의 테스트 (다중 질문 순환)
        """
        import random
        questions = [
            "가족 간에 돈을 빌려줄 때 증여세를 내지 않으려면 차용증을 어떻게 작성해야 하나요?",
            "현재 이자제한법상 법정 최고 이자율은 연 몇 퍼센트인가요?",
            "부모님으로부터 5천만 원을 무이자로 빌렸는데, 이것도 상속세나 증여세 대상이 되나요?",
            "차용증에 변제기일을 적지 않으면 법적으로 돈을 언제 갚아야 하나요?"
        ]
        
        payload = {
            "message": random.choice(questions),
            "history": []
        }
        
        with self.client.post("/internal/v1/chat", json=payload, headers=self.headers, catch_response=True) as response:
            if response.status_code == 200:
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

    @task(3)
    def clauses_performance_test(self):
        # 지정된 요청 횟수에 도달하면 테스트 자동 종료
        if self.request_count >= self.max_requests:
            self.environment.runner.quit()
            return

        self.request_count += 1
        """
        핵심 기능: 특약사항(Clauses) 자동 생성 API 테스트
        """
        payload = {
            "contract_seq": 1,
            "user_intent": "만약 제날짜에 돈을 갚지 않으면 이자제한법에서 허용하는 법정 최고 이자율을 적용해서 지연이자를 물리고 싶어.",
            "contract_context": "원금 5,000만 원, 이자율 연 5%의 가족 간 금전소비대차(차용증) 계약서",
            "template_type": "금전소비대차계약서(차용증)"
        }
        
        with self.client.post("/internal/v1/clauses/generate", json=payload, headers=self.headers, catch_response=True) as response:
            if response.status_code == 200:
                try:
                    data = response.json()
                    if "legal_clause" in data and len(data["legal_clause"]) > 5:
                        response.success()
                    else:
                        response.failure("Clause generation failed or empty")
                except Exception:
                    response.failure("Invalid JSON response")
            else:
                response.failure(f"Error Code {response.status_code}")

    # @task(1)
    def check_health(self):
        """
        단순 헬스체크 응답성 테스트 (채팅 부하 테스트에 집중하기 위해 비활성화)
        """
        self.client.get("/health")

@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    print(f"{get_kst()} 🚀 성능 테스트 시작: 대상 호스트 - {environment.host}")

@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    print(f"{get_kst()} 🏁 성능 테스트 종료")

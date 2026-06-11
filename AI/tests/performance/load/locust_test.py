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
INTERNAL_API_KEY = os.getenv("INTERNAL_API_KEY", "")
TARGET_ENDPOINT = "/internal/v1/chat"

class AIServiceUser(HttpUser):
    """
    EC2 AI 서비스(Billage) 성능 측정을 위한 Locust 시뮬레이터
    (유저 1명당 채팅 1회, 특약 1회를 순차적으로 요청 후 자동 종료)
    """
    # 기본 호스트 설정 (tests/.env 또는 기본값)
    host = os.getenv("TEST_BASE_URL", "http://localhost:8000")
    
    # 쉴 틈 없이 맹공격하도록 대기 시간을 0초로 설정
    wait_time = constant(0)

    def on_start(self):
        """
        테스트 시작 시 인증 토큰 및 초기 설정
        """
        self.internal_api_key = os.getenv("INTERNAL_API_KEY", "")
        self.is_done = False
        print(f"{get_kst()} 🚀 성능 테스트 시작: 대상 호스트 - {self.host} (채팅 1회, 특약 1회 순차 요청 후 자동 종료)")
        self.headers = {
            "X-Internal-API-Key": self.internal_api_key,
            "Content-Type": "application/json"
        }

    @task
    def sequential_performance_test(self):
        # 한 번 순서대로 실행한 유저는 더 이상 실행하지 않고 종료
        if self.is_done:
            self.environment.runner.quit()
            return
            
        self.is_done = True

        # 1. 채팅 성능 테스트 우선 실행
        self.chat_performance_test()
        
        # 2. 이어서 특약 생성 성능 테스트 실행
        self.clauses_performance_test()
        
        # 3. 유저 역할 완료 시 이 사용자 인스턴스를 종료
        print(f"{get_kst()} 🏁 유저 1명 목표 달성(총 2회 호출)하여 테스트를 자동 종료합니다.")
        self.environment.runner.quit()

    def chat_performance_test(self):
        """
        핵심 기능 1: RAG 기반 챗봇 질의 테스트 (다중 질문 순환)
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

    def clauses_performance_test(self):
        """
        핵심 기능 2: 특약사항(Clauses) 자동 생성 API 테스트
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

    # def check_health(self):
    #     \"\"\"
    #     단순 헬스체크 응답성 테스트 (채팅 부하 테스트에 집중하기 위해 비활성화)
    #     \"\"\"
    #     self.client.get("/health")

@events.test_start.add_listener
def on_test_start(environment, **kwargs):
    print(f"{get_kst()} 🚀 성능 테스트 시작: 대상 호스트 - {environment.host}")

@events.test_stop.add_listener
def on_test_stop(environment, **kwargs):
    print(f"{get_kst()} 🏁 성능 테스트 종료")

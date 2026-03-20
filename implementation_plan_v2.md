# [EC2 AI 서비스 성능 측정 및 개선 계획]

## 개요
실 서비스 환경(EC2)에서 AI 서버의 성능을 객관적으로 측정하고, 병목 현상을 파악하여 개선하기 위한 상세 테스트 계획입니다. 단순 응답 속도뿐만 아니라 RAG(Retrieval-Augmented Generation) 파이프라인의 각 단계별 품질을 다각도로 분석하고 체계적인 테스트 환경을 구축합니다.

---

## 1. 실전 대응력 강화를 위한 4가지 핵심 보완 사항

실 서비스 환경에서의 변수를 고려하여 아래 4가지 측면에서 심화 측정을 수행합니다.

### 1-1. 부하 및 스트레스 테스트 (Load & Stress Test)
- **동시성 측정 (Concurrency):** 10~100명의 동시 사용자를 가정하여 **TPS(Transactions Per Second)**와 응답 지연 임계점 파악.
- **자원 임계치 파악:** CPU 사용량 80% 도달 시점을 확인하여 EC2 **Auto-scaling 확장 트리거** 기준으로 활용.

### 1-2. 데이터 시나리오 다변화 (Edge Case Testing)
- **Long Context 테스트:** 토큰 제한(Token Limit) 근접 시 임베딩 및 생성 시간 급증 여부 확인.
- **Cold Start vs. Warm-up:** 인스턴스 재시작 직후(모델 로드 전)와 로드 후의 응답 속도 차이 기록 (UX 가이드 마련).

### 1-3. RAG 파이프라인 '실패 지점' 분석 (Failure Mode Analysis)
| 분석 항목 | 설명 | 대응 방안 |
| :--- | :--- | :--- |
| **Retrieval Failure** | 정답 문서 누락에 따른 오답 발생 시 | Top-k 조정 및 임베딩 모델 재검토 |
| **Context Overflow** | 문맥은 맞으나 LLM이 정답을 놓칠 시 | 프롬프트 엔지니어링 및 컨텍스트 압축 |
| **API Timeout Rate** | 외부 LLM API 응답 실패 시 | Retry 로직 및 서킷 브레이커 도입 |

### 1-4. 비용 최적화 지표 (Cost-Efficiency)
- **Token Efficiency:** 답변 1자당 소비 토큰 수를 계산하여 프롬프트 효율성 점검.
- **Cache Hit Rate:** 캐싱 적용 시 동일 질문에 대한 서버 부하 감소 효과 수치화.

---

## 2. 테스트 디렉토리 구조 및 유형

체계적인 성능 관리 및 Jenkins 연동을 위해 프로젝트 루트에 아래 구조를 도입합니다.

```text
/ (Project Root)
├── ai_service/                # 실제 서비스 소스 코드
└── tests/                     # 테스트 최상위 디렉토리
    ├── unit/                  # 단위 테스트 (함수/클래스 단위, 예: RAG 로직 검증)
    ├── integration/           # 통합 테스트 (API 엔드포인트 호출 및 보안 검증)
    ├── performance/           # 성능 관련 테스트 모음
    │   ├── load/              # 부하 테스트
    │   ├── stress/            # 스트레스 테스트
    │   ├── endurance/         # 내구량 테스트
    │   └── reports/           # 결과 보고서
    └── utils/                 # 공통 유틸리티 (데이터 생성기, 헬퍼 등)
```

---

## 3. Jenkins 파이프라인 연동 및 시각화

배포 단계(`Deploy`) 이후에 **성능 및 품질 테스트 단계**를 추가하여 자동화를 실현합니다.

### 3-1. 테스트 자동 실행 및 결과 수집
- `tests/` 내의 모든 테스트(`unit`, `integration`, `performance`)를 Jenkins가 자동으로 실행합니다.
- 특히 성능 테스트 결과는 `reports/` 폴더에 생성되어 아카이빙됩니다.

### 3-2. 성능 추이 그래프 (Performance Trend)
- 빌드 회차별 레이턴시, TPS, 메모리 사용량 변화를 시각화하여 품질 변화를 한눈에 파악합니다.

---

## 4. 검증 계획 (최종)
### 자동화 테스트
- `pytest`를 활용한 단위/통합 테스트 수행.
- `Locust/K6` 기반의 부하/스트레스/내구량 테스트 시나리오 실행.
- **GMS API 연계 실전 데이터셋 생성:** 하드코딩된 데이터 대신 GMS API를 통해 수집된 질문 데이터 활용.

### 수동 검증
- 테스트 중 실시간 리소스 모니터링(htop, CloudWatch 등).
- 내구량 테스트 전후의 시스템 메모리 점유율 비교 분석.

---

## 5. 상세 구현 기술 가이드 (v2 추가 내용)

### 5-1. GMS API 연계 데이터 생성기 (`tests/utils/generator.py`)
하드코딩된 질문 대신, GMS API를 통해 실무에서 발생할 법한 질문 리스트를 자동 생성하거나 수집합니다.

```python
# tests/utils/generator.py 예시
async def generate_test_queries(count=50):
    # GMSGeminiClient를 활용하여 "부동산 법률 관련 질문 50개 생성" 요청
    # 생성된 데이터를 tests/utils/test_queries.json으로 저장
```

### 5-2. 부하 테스트 구현 상세 (Locust)
Python 기반의 **Locust**를 사용하여 동시 가상 유저를 생성하고 TPS를 측정합니다.
(기존 내용 유지)

### 5-3. 단위 및 통합 테스트 (Pytest)
RAG 엔진의 검색 정확도와 API 보안을 검증합니다.

```python
# tests/unit/test_rag_engine.py
def test_search_accuracy():
    engine = get_rag_engine()
    context = engine.search_similar_documents("전세사기", k=1)
    assert "전세" in context

# tests/integration/test_api.py
def test_chat_endpoint_security():
    # API Key 없이 요청 시 403 Forbidden 확인
    response = requests.post(CHAT_URL, json={...})
    assert response.status_code == 403
```

### 5-4. RAG 실패 지점 분석 로직
(기존 내용 유지: track_component_latency 데코레이터 등)

### 5-4. 비용 및 효율성 측정 (Token Logging)
LLM API의 응답 객체에서 사용된 토큰 정보를 가로채어 리포트용 JSON에 누적합니다.
```python
# ai_service/app/services/chatbot.py 내 응답 처리부
usage = response.usage
token_data = {
    "prompt": usage.prompt_tokens,
    "completion": usage.completion_tokens,
    "total": usage.total_tokens
}
# 이 데이터를 reports/cost_metrics.json에 빌드 번호와 함께 기록
```

### 5-5. Jenkinsfile 스테이지 구성 예시
```groovy
stage('Performance Test') {
    steps {
        sh '''
            # 테스트 환경 설정
            pip install locust ragas
            
            # 1. 부하 테스트 실행 (결과 csv 생성)
            locust -f tests/performance/load/locust_test.py --headless -u 10 -r 2 --run-time 1m --csv tests/performance/reports/load_result
            
            # 2. RAG 품질 측정 실행
            python tests/performance/scripts/run_ragas.py
        '''
    }
    post {
        always {
            archiveArtifacts artifacts: 'tests/performance/reports/**'
            perfReport sourceDataFiles: 'tests/performance/reports/*.csv'
        }
    }
}
```

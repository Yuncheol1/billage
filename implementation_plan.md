# [EC2 AI 서비스 성능 측정 및 개선 계획]

## 개요
실 서비스 환경(EC2)에서 AI 서버의 성능을 객관적으로 측정하고, 병목 현상을 파악하여 개선하기 위한 상세 테스트 계획입니다. 단순 응답 속도뿐만 아니라 RAG(Retrieval-Augmented Generation) 파이프라인의 각 단계별 품질을 다각도로 분석합니다.

## 측정 항목 (KPI)

사용자가 제시한 **검색 정확도**와 **응답 속도** 외에, 보다 심층적인 분석을 위해 아래 항목들을 추가하여 측정합니다.

### 1. 검색 품질 (Retrieval Performance)
- **Hit Rate (k=1, 3, 5):** 질문에 대한 정답 문서가 검색 결과 TOP-k 내에 포함될 확률.
- **MRR (Mean Reciprocal Rank):** 정답 문서가 검색 결과 중 몇 번째에 위치하는지에 대한 지표.
- **Context Precision:** 검색된 문서들이 질문과 얼마나 밀접하게 연관되어 있는지 측정.

### 2. 생성 품질 (Generation Quality)
- **Faithfulness (충실도):** LLM의 답변이 제공된 컨텍스트(법률/판례)에 근거하고 있는지 (환각 현상 체크).
- **Answer Relevance (답변 관련성):** 답변이 사용자의 질문 의도에 부합하는지 측정.

### 3. 응답 속도 및 효율성 (Latency & Efficiency)
- **세부 구간별 Latency 측정:**
  - **Query Embedding:** 질문을 임베딩 모델로 변환하는 데 소요되는 시간.
  - **Vector Search:** ChromaDB에서 유사 문서를 검색하는 데 소요되는 시간.
  - **Generation:** LLM이 최공 답변을 생성하는 데 소요되는 시간 (OpenAI/Gemini API 호출 시간 포함).
- **End-to-End Latency:** 전체 프로세스 시작부터 완료까지의 총 소요 시간.

### 4. 리소스 및 비용 (Resource & Cost)
- **CPU/RAM Usage:** EC2 인스턴스의 자원 점유율 (특히 CPU 모드 임베딩 로드 시 부하 확인).
- **Token Usage:** 요청당 소모되는 토큰 수와 비용 추산.

---

## [추가] 실전 대응력 강화를 위한 4가지 핵심 보완 사항

사용자 제안에 따라 실 서비스 환경에서의 변수를 고려한 심화 측정 항목을 추가합니다.

### 1. 부하 및 스트레스 테스트 (Load & Stress Test)
- **동시성 측정 (Concurrency):** 10~100명의 동시 사용자를 가정하여 **TPS(Transactions Per Second)**와 응답 지연 임계점 파악.
- **자원 임계치 파악:** CPU 사용량 80% 도달 시점을 확인하여 EC2 **Auto-scaling 확장 트리거** 기준으로 활용.

### 2. 데이터 시나리오 다변화 (Edge Case Testing)
- **Long Context 테스트:** 토큰 제한(Token Limit) 근접 시 임베딩 및 생성 시간 급증 여부 확인.
- **Cold Start vs. Warm-up:** 인스턴스 재시작 직후(모델 로드 전)와 로드 후의 응답 속도 차이 기록 (UX 가이드 마련).

### 3. RAG 파이프라인 '실패 지점' 분석 (Failure Mode Analysis)
| 분석 항목 | 설명 | 대응 방안 |
| :--- | :--- | :--- |
| **Retrieval Failure** | 정답 문서 누락에 따른 오답 발생 시 | Top-k 조정 및 임베딩 모델 재검토 |
| **Context Overflow** | 문맥은 맞으나 LLM이 정답을 놓칠 시 | 프롬프트 엔지니어링 및 컨텍스트 압축 |
| **API Timeout Rate** | 외부 LLM API 응답 실패 시 | Retry 로직 및 서킷 브레이커(Circuit Breaker) 도입 |

### 4. 비용 최적화 지표 (Cost-Efficiency)
- **Token Efficiency:** 답변 1자당 소비 토큰 수를 계산하여 프롬프트 효율성 점검.
- **Cache Hit Rate:** 캐싱 적용 시 동일 질문에 대한 서버 부하 감소 효과 수치화.

---

## Jenkins 파이프라인 연동 및 시각화

기본 배포 단계(`Deploy`) 이후에 **성능 테스트 및 시각화 단계**를 추가합니다.

### 1. 성능 추이 그래프 (Performance Trend)
- [Performance Plugin](https://plugins.jenkins.io/perfpublisher/) 등을 활용하여 빌드 회차별 레이턴시 및 정확도 변화를 시각화.
- 특정 업데이트 이후 성능 저하 발생 시 즉각 탐지 가능하도록 구성.

### 2. 테스트 결과 리포트 저장
- `archiveArtifacts`를 사용하여 HTML/JSON 형태의 상세 리포트 보존.

---

## 검증 계획 (업데이트)
### 자동화 테스트
- `Locust` 또는 `K6`를 활용한 부하 테스트 스크립트 실행.
- `Ragas`를 활용한 정성적 품질(Faithfulness 등) 측정 스크립트 실행.
- Jenkins 빌드 결과에 성능 추이 그래프 및 상세 리포트 첨부.

### 수동 검증
- 실시간 리소스 모니터링(CloudWatch 또는 htop)을 통한 인스턴스 상태 확인.
- 롱 컨텍스트 입력 시의 응답 일관성 점검.

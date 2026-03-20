# [실전 가이드] AI 서비스 성능 및 품질 테스트 따라하기

이 문서는 구축된 테스트 환경을 사용하여 **데이터 생성 -> 기능 검증 -> 부하 측정 -> 자동화**까지의 전 과정을 단계별로 안내합니다.

---

## 🚀 0단계: 사전 준비 (최초 1회)

테스트에 필요한 라이브러리를 로컬 PC와 Jenkins 서버에 설치합니다.

```bash
# 로컬 터미널에서 실행
pip install locust requests httpx
```

## 📝 1단계: 테스트 환경 설정 (`tests/.env`)

테스트를 수행하기 전, 대상 서버의 주소와 인증 키를 설정합니다.

1.  **`tests/.env` 파일 열기**
2.  **내용 수정:**
    ```env
    # 실제 서비스를 제공 중인 EC2의 퍼블릭 IP를 입력하세요
    TEST_BASE_URL=http://<EC2-IP>:8000

    # ai_service/.env의 INTERNAL_API_KEY와 동일하게 설정하세요
    INTERNAL_API_KEY=super-secret-key-change-me-in-production
    ```

---

## 📝 2단계: 실전 테스트 데이터 생성 (GMS API 연계)

하드코딩된 데이터 대신 AI(Gemini)가 생성한 실무 질문셋을 준비합니다.

1. **실행 명령:**  
   (프로젝트 루트 디렉토리에서 실행)
   ```bash
   python tests/utils/generator.py
   ```
2. **결과 확인:**  
   `tests/utils/test_queries.json` 파일이 생성되었는지 확인하세요. 실제 사용자가 물어볼 법한 법률/세무 질문 20개가 담겨 있습니다.

---

## ✅ 3단계: 기능 및 보안 검증 (단위/통합 테스트)

서버의 로직과 API 보안이 정상인지 확인합니다.

1. **단위 테스트 (RAG 엔진 검증):**
   ```bash
   python tests/unit/test_rag_engine.py
   ```
   *   **성공 시:** `OK` 메시지가 출력됩니다.

2. **통합 테스트 (API 보안 및 응답성):**  
   (서버가 구동 중인 상태에서 실행)
   ```bash
   python tests/integration/test_api.py
   ```
   *   **체크포인트:** API Key 없이 접근 시 `403 Forbidden`이 정상적으로 뜨는지 확인합니다.

---

## 🔥 4단계: 부하 테스트 실행 (Locust)

우리 서버가 얼마나 많은 동시 접속자를 버티는지 측정합니다.

1. **테스트 실행:**
   ```bash
   # <EC2-IP> 자리에 실제 서버 주소를 입력하세요
   locust -f tests/performance/load/locust_test.py --host http://<EC2-IP>:8000
   ```
2. **웹 UI 접속:**  
   브라우저에서 `http://localhost:8089`에 접속합니다.
3. **설정값 입력:**
   - **Number of users:** 20 (동시 접속 수)
   - **Spawn rate:** 2 (초당 증가 수)
4. **결과 분석:**  
   - `Statistics` 탭에서 **Average Response Time**과 **Requests/s (TPS)**를 확인합니다.
   - `Charts` 탭에서 시간에 따른 응답 속도 변화를 관찰합니다.

---

## 🤖 5단계: Jenkins 자동화 연동

배포 시마다 성능을 자동으로 측정하고 기록을 남깁니다.

1. **Jenkinsfile 수정:**  
   `Jenkinsfile-ai`의 `Deploy` 단계 아래에 다음 내용을 추가합니다 (이미 계획서 v2에 포함됨).
   ```groovy
   stage('Performance Test') {
       steps {
           sh 'python tests/performance/scripts/run_perf_test.py --host http://localhost:8000 --users 10'
       }
       post {
           always {
               archiveArtifacts artifacts: 'tests/performance/reports/**'
           }
       }
   }
   ```
2. **결과 리포트:**  
   Jenkins 빌드 화면의 **Artifacts** 영역에서 생성된 상세 리포트(CSV/JSON)를 다운로드할 수 있습니다.

---

## 💡 팁 및 가이드
- **Long Context 테스트:** `tests/utils/test_queries.json`에 아주 긴 질문을 수동으로 추가하여 AI의 답변 속도 저하를 테스트해 보세요.
- **Fail 발생 시:** `locust` 화면의 `Failures` 탭에서 403 에러가 뜨면 `INTERNAL_API_KEY` 환경변수가 잘못 설정된 것입니다.

**이제 가이드를 따라 1단계부터 차례대로 진행해 보세요!**

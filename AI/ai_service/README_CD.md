# 🚀 AI Service CI/CD 배포 가이드 (인프라 담당자용)

본 문서는 `ai_service` 마이크로서비스의 자동 배포를 위해 인프라 담당자가 숙지해야 할 설정을 기록합니다.

## 1. 기술 스택 및 환경
- **Language**: Python 3.10+
- **Framework**: FastAPI (Uvicorn)
- **Database**: ChromaDB (Vector DB, Local File-based)
- **Model**: BAAI/bge-m3 (HuggingFace, 약 2.2GB)

## 2. 하드웨어 리소스 및 메모리 관리 전략
> [!IMPORTANT]
> 전체 서버 메모리가 16GB이며, **Spring Backend와 동일 인스턴스에서 구동**될 경우 다음과 같은 메모리 할당 전략을 권장합니다.

- **AI Service (FastAPI)**: 약 **5~6GB** 점유 예상 (모델 로딩 및 인덱스 상주)
- **Spring Backend**: 약 **4~6GB** (JVM Heap `-Xmx4g` 등 제한 설정 권장)
- **OS 및 기타**: 약 2~4GB 여유분

### [메모리 부족(OOM) 방지 대책]
1. **JVM Heap 제한**: Spring 가동 시 `-Xms4g -Xmx4g` 등으로 메모리 상한선을 확실히 제한해 주세요.
2. **Swap 활용**: 예상치 못한 급격한 메모리 사용량 증가에 대비해 **2~4GB 정도의 Swap 파티션** 설정을 권장합니다.
3. **리소스 분리 (권장)**: 만약 서비스 안정성이 최우선이라면, Docker 컨테이너 실행 시 `--memory="8g"` 옵션으로 각 서비스의 최대 가용량을 나누는 것이 안전합니다.

## 3. 필수 환경 변수 (Environment Variables)
배포 서버의 환경 변수(또는 `.env`)에 다음 항목이 설정되어야 합니다.

| 변수명 | 설명 | 비고 |
|:---|:---|:---|
| `GMS_KEY` | SSAFY GMS Gateway 전용 API 키 | Gemini 모델 호출용 |
| `INTERNAL_API_KEY` | 백엔드(Spring) - AI 서비스 간 인증 키 | `X-Internal-Api-Key` 헤더 검증용 |
| `API_V1_STR` | API 경로 프리픽스 | 기본값: `/internal/v1` |
| `CHROMA_DB_DIR` | 벡터 DB 저장 경로 | 기본값: `./chroma_db` |

## 4. 데이터 영속성 (Data Persistence)
본 서비스의 핵심 데이터인 벡터 DB는 **Embedded DB(파일 기반)** 방식으로 작동합니다.

### [작동 방식 및 구조]
- **저장 경로**: `./chroma_db/`
- **주요 파일**: 
  - `chroma.sqlite3`: 메타데이터 및 문서 원문 저장
  - `{UUID}/`: 실제 임베딩 벡터 데이터 및 검색 인덱스 파일들
- **현재 상태**: 수백 건 이상의 법령/판례 데이터가 이미 임베딩되어 저장된 상태입니다.

### [Persistent Volume(PV) 마운트가 필수인 이유]
1. **휘발성 방지**: Docker/Kubernetes 환경에서 컨테이너 내부의 파일 시스템은 일시적(Ephemeral)입니다. 컨테이너가 업데이트되거나 재시작되면 내부 데이터는 모두 소실됩니다.
2. **비용 및 시간 절감**: 전체 데이터를 다시 임베딩하는 작업은 API 호출 비용(Gemini 등)이 발생하며, 모델 로딩 및 연산에 상당한 시간(수십 분 이상)이 소요됩니다. 
3. **일관성 유지**: 인덱싱된 데이터가 유실되면 검색 결과가 나오지 않게 되므로, **반드시 해당 디렉토리를 외부 서버 스토리지(EBS, NAS 등)에 바인딩**하거나 볼륨 마운트해야 합니다.

## 5. 배포 파이프라인 구성 제안
1. **Build**: `Dockerfile`을 기반으로 이미지 빌드
2. **Deploy**: 새로운 컨테이너 구동
3. **Health Check**: `GET /health` 엔드포인트 호출 (HTTP 200 응답 확인)
   - *주의*: 서버 구동 시 모델 로딩으로 인해 헬스체크 성공까지 약 **1~2분 정도의 웜업 시간**이 필요할 수 있습니다.

## 6. 보안 정책
- 모든 API 요청은 Header에 `X-Internal-Api-Key`가 포함되어야 합니다.
- 외부 노출을 차단하고, Spring API Gateway 혹은 특정 내부 IP 대역에서만 접근 가능하도록 방화벽 설정을 권장합니다.

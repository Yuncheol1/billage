# 임베딩 모델 벤치마킹 수행 보고서

본 보고서는 한국어 법률/금융 도메인의 테스트 셋팅 환경에서 수행된 3가지 후보 임베딩 모델의 벤치마킹 결과입니다.

## 1. 종합 성능 비교 표

| Model | Hit Rate@3 | MRR | Latency (ms) | Throughput | Peak VRAM |
| --- | --- | --- | --- | --- | --- |
| BAAI/bge-m3 | 85.0% | 0.775 | 57.9ms | 129.4 doc/s | 1.27 GB |
| jhgan/ko-sroberta-multitask | 61.0% | 0.552 | 50.2ms | 322.6 doc/s | 1.31 GB |
| intfloat/multilingual-e5-large | 64.0% | 0.603 | 81.9ms | 131.1 doc/s | 2.50 GB |

## 2. 측정 지표 설명
- **Hit Rate@3**: 상위 3개 검색 결과 중 정답이 포함된 비율
- **MRR(Mean Reciprocal Rank)**: 정답의 평균 순위 (1위에 가까울수록 1.0)
- **Latency (ms)**: 모델의 `query` 테스트 1회당 소요되는 평균 시간(초 단위 → ms)
- **Throughput**: 1초당 임베딩 변환 및 인덱싱한 문서(조항) 수
- **Peak VRAM**: 인코딩 과정에서 GPU 메모리가 최대로 할당된 수량

## 3. 평가 요약
> 결과에 따라 서비스 특성(비용 최우선 vs 성능 최우선)에 맞춰 1가지 모델을 최종 시스템에 반영할 수 있습니다.

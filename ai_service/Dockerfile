<<<<<<< HEAD
# Python 3.10 슬림 이미지를 베이스로 사용
FROM python:3.10-slim

# 작업 디렉토리 설정
WORKDIR /app

# 필수 라이브러리 설치 (컴파일러 등 필요한 경우 대비)
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# 의존성 파일 복사 및 설치
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 애플리케이션 코드 및 데이터 복사
COPY . .

# 환경 변수 기본값 설정 (필요 시 빌드 타임 혹은 런타임에 오버라이드)
ENV API_V1_STR="/internal/v1"
ENV CHROMA_DB_DIR="./chroma_db"

# 포트 노출
EXPOSE 8000

# 서버 실행 (Uvicorn)
# --host 0.0.0.0으로 설정해야 컨테이너 외부에서 접근 가능
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
=======
# ai/Dockerfile
FROM python:3.11-slim

WORKDIR /app

# 의존성 먼저 복사 (캐시 활용)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 소스 복사
COPY . .

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
>>>>>>> 7649493b9571aa7aa394e02c5e65eee29ff0f494

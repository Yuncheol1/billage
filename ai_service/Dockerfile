# ai/Dockerfile
FROM python:3.11-slim

WORKDIR /app

# 컨테이너 시간을 한국 시간(KST)으로 고정합니다.
ENV TZ=Asia/Seoul
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# HuggingFace 및 PyTorch 멀티스레드 데드락 방지 옵션
ENV TOKENIZERS_PARALLELISM=false
ENV OMP_NUM_THREADS=1

# 의존성 먼저 복사 (캐시 활용)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# 소스 복사
COPY . .

EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
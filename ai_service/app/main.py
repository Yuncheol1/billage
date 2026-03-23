from fastapi import FastAPI, Depends, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import HTMLResponse, FileResponse
import os
import logging
from contextlib import asynccontextmanager
from app.api.internal_v1.router import internal_router
from app.core.config import settings
from app.core.security import verify_internal_api_key
from app.services.rag_engine import get_rag_engine

# --- 로깅 시간 출력(KST) 전역 설정 ---
log_format = "%(asctime)s [%(levelname)s] %(message)s"
date_format = "%Y-%m-%d %H:%M:%S KST"
formatter = logging.Formatter(log_format, datefmt=date_format)

logging.basicConfig(level=logging.INFO, format=log_format, datefmt=date_format)
logger = logging.getLogger(__name__)

# Uvicorn의 내부 로거 포맷을 강제로 덮어씌워서 시간 출력 적용
for name in ("uvicorn", "uvicorn.error", "uvicorn.access"):
    u_logger = logging.getLogger(name)
    for handler in u_logger.handlers:
        handler.setFormatter(formatter)
# --------------------------------------

@asynccontextmanager
async def lifespan(app: FastAPI):
    """
    [EC2 서버 구동 최적화]
    서버 부팅 시 약 2GB가 넘는 bge-m3 임베딩 모델과 ChromaDB 인스턴스를 
    16GB RAM에 단 한 번만 로드하여 상주(Caching)시킵니다.
    이 조치가 없으면 API 호출 때마다 수 밀리초 이상의 모델 웜업 지연 현상이 발생합니다.
    """
    logger.info("[Lifespan] FastAPI 서버 시작: RAG 엔진과 임베딩 모델을 16GB RAM에 캐싱합니다...")
    get_rag_engine() # 싱글톤 인스턴스 초기화 트리거
    logger.info("[Lifespan] 모델 로딩 완료! 요청 받을 준비가 되었습니다.")
    yield
    logger.info("[Lifespan] FastAPI 서버가 종료됩니다. 메모리를 반환합니다...")

app = FastAPI(
    title=settings.PROJECT_NAME,
    lifespan=lifespan,
    openapi_url=f"{settings.API_V1_STR}/openapi.json",
    description="Spring API Gateway에서만 호출 가능한 B2B 내부망 전용 AI 마이크로서비스 엔진",
)

# 로컬 HTML 테스트 파일을 위한 CORS 임시 허용
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 실제 운영 시에는 프론트/Spring 도메인만 허용하도록 변경 필요
    allow_credentials=False, # 와일드카드(*) 사용 시 브라우저 정책상 credentials(쿠키 등) 허용 불가
    allow_methods=["*"],
    allow_headers=["*"],
)

# 내부 연동(Internal) 라우터 전역에 API KEY 검증 의존성(Dependency)을 강제 주입
# Client가 직접 URL로 접근할 경우 Header에 Internal API Key가 없으면 403 에러 반환
app.include_router(
    internal_router, 
    prefix=settings.API_V1_STR, 
    dependencies=[Depends(verify_internal_api_key)]
)

# ── 테스트 전용 UI 서빙 (Internal전용) ────────────────────────────────────
# static 폴더 내의 index.html, app.js, style.css를 직접 서빙합니다.
# 배포 후 http://domain:port/internal/test 로 접속하여 바로 테스트 가능
current_dir = os.path.dirname(os.path.abspath(__file__))
static_dir = os.path.join(os.path.dirname(current_dir), "static")

# 디버그용 출력
logger.info(f"[Init] Static Directory Path: {static_dir}")

if os.path.exists(static_dir):
    app.mount("/static", StaticFiles(directory=static_dir), name="static")

@app.get("/internal/test", response_class=HTMLResponse, tags=["Internal-Test"])
async def get_test_ui():
    """테스트용 UI인 index.html 파일을 반환합니다."""
    index_path = os.path.join(static_dir, "index.html")
    if os.path.exists(index_path):
        return FileResponse(index_path)
    return HTMLResponse(content=f"<h1>Test UI Not Found at {static_dir}</h1>", status_code=404)

@app.get("/health", tags=["Health"])
def read_health():
    """서버 구동 및 로드밸런서 상태 체크용 단순 엔드포인트"""
    return {"status": "ok"}

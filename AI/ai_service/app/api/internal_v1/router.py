from fastapi import APIRouter
from app.api.internal_v1.endpoints import analyze, clauses, chat

internal_router = APIRouter()

# 3개의 핵심 기능 Router 각각에 Prefix 부여
internal_router.include_router(analyze.router, prefix="/analyze", tags=["Analyze"])
internal_router.include_router(clauses.router, prefix="/clauses", tags=["Clauses"])
internal_router.include_router(chat.router, prefix="/chat", tags=["ChatBot"])

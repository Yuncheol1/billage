import redis.asyncio as redis
from app.core.config import settings
import logging

logger = logging.getLogger(__name__)

class RedisClient:
    _instance = None
    _redis = None

    @classmethod
    async def get_instance(cls):
        if cls._instance is None:
            cls._instance = RedisClient()
            try:
                cls._redis = redis.Redis(
                    host=settings.REDIS_HOST,
                    port=settings.REDIS_PORT,
                    db=settings.REDIS_DB,
                    decode_responses=True
                )
                # 연결 테스트
                await cls._redis.ping()
                logger.info(f"✅ Redis 연결 성공: {settings.REDIS_HOST}:{settings.REDIS_PORT}")
            except Exception as e:
                logger.error(f"❌ Redis 연결 실패: {str(e)}")
                cls._redis = None
        return cls._redis

    @classmethod
    async def close(cls):
        if cls._redis:
            await cls._redis.close()
            logger.info("🔌 Redis 연결 종료")

# 전역적으로 사용할 Redis 인스턴스 가져오기 도우미
async def get_redis():
    return await RedisClient.get_instance()

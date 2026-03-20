import logging
import numpy as np
from redisvl.index import SearchIndex
from redisvl.schema import IndexSchema
from redisvl.query import VectorQuery
from app.core.config import settings
from app.core.redis_client import RedisClient

logger = logging.getLogger(__name__)

# RedisVL 스키마 정의 (시맨틱 캐시용)
SCHEMA = {
    "index": {
        "name": "semantic_cache",
        "prefix": "cache",
    },
    "fields": [
        {"name": "original_query", "type": "text"},
        {"name": "answer", "type": "text"},
        {
            "name": "query_vector",
            "type": "vector",
            "attrs": {
                "dims": 1024, # BAAI/bge-m3의 기본 차원
                "distance_metric": "cosine",
                "algorithm": "flat", # 소규모 캐시에는 flat이 정확함
                "datatype": "float32"
            }
        }
    ]
}

class SemanticCacheManager:
    def __init__(self):
        self.index = None
        self.schema = IndexSchema.from_dict(SCHEMA)
        self.threshold = settings.SEMANTIC_CACHE_THRESHOLD

    async def _init_index(self):
        """인덱스 초기화 및 Redis 연결 확인"""
        if self.index is None:
            try:
                redis_conn = await RedisClient.get_instance()
                if redis_conn is None:
                    return False
                
                # RedisVL 전용 인스턴스 생성 (redis_url 필요)
                redis_url = f"redis://{settings.REDIS_HOST}:{settings.REDIS_PORT}/{settings.REDIS_DB}"
                self.index = SearchIndex(self.schema, redis_url=redis_url)
                
                # 인덱스가 없으면 생성
                if not self.index.exists():
                    self.index.create(overwrite=False)
                    logger.info("🆕 Redis 시맨틱 캐시 인덱스가 생성되었습니다.")
                return True
            except Exception as e:
                logger.error(f"❌ 캐시 엔진 초기화 실패: {str(e)}")
                return False
        return True

    async def get_cache(self, query_vector: list) -> str:
        """
        벡터 유사도 검색을 통해 캐시된 답변 반환
        """
        if not await self._init_index():
            return None

        try:
            # 벡터 쿼리 구성
            query = VectorQuery(
                vector=query_vector,
                vector_field_name="query_vector",
                return_fields=["answer", "vector_distance"],
                num_results=1
            )
            
            results = self.index.query(query)
            
            if results:
                match = results[0]
                # RedisVL's vector_distance for cosine is (1 - similarity)
                similarity = 1 - float(match['vector_distance'])
                
                if similarity >= self.threshold:
                    logger.info(f"🎯 시맨틱 캐시 히트! (유사도: {similarity:.4f})")
                    return match['answer']
                
            return None
        except Exception as e:
            logger.error(f"⚠️ 캐시 조회 중 오류: {str(e)}")
            return None

    async def set_cache(self, query_vector: list, original_query: str, answer: str):
        """
        새로운 질문과 답변을 캐시에 저장
        """
        if not await self._init_index():
            return

        try:
            data = [{
                "original_query": original_query,
                "answer": answer,
                "query_vector": np.array(query_vector, dtype=np.float32).tolist()
            }]
            self.index.load(data)
            logger.info("💾 새로운 응답이 시맨틱 캐시에 저장되었습니다.")
        except Exception as e:
            logger.error(f"⚠️ 캐시 저장 실패: {str(e)}")

# 전역 인스턴스
cache_manager = SemanticCacheManager()

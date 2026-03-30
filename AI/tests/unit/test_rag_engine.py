import sys
import unittest
from pathlib import Path

# --- 프로젝트 루트 설정 ---
PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
AI_SERVICE_DIR = PROJECT_ROOT / "ai_service"
sys.path.append(str(AI_SERVICE_DIR))

# RAG 엔진 임포트
from app.services.rag_engine import get_rag_engine

class TestRAGEngine(unittest.TestCase):
    """
    RAG 엔진의 검색 기능 단위 테스트
    """
    @classmethod
    def setUpClass(cls):
        """테스트 시작 전 RAG 엔진 초기화 (모델 로딩 포함)"""
        print("\n[Unit Test] RAG 엔진 초기화 중...")
        cls.engine = get_rag_engine()

    def test_search_return_type(self):
        """검색 결과가 문자열(Context) 형식인지 확인"""
        query = "전세사기 예방"
        context = self.engine.search_similar_documents(query, k=1)
        self.assertIsInstance(context, str)
        self.assertGreater(len(context), 0)

    def test_search_not_found(self):
        """관련 없는 검색어에 대한 예외 처리 확인 (데이터에 없는 무의미한 검색어)"""
        query = "asdfghjkl123456789"
        context = self.engine.search_similar_documents(query, k=1)
        # 검색 결과가 없더라도 에러가 나지 않고 문자열을 반환해야 함
        self.assertIsInstance(context, str)

if __name__ == "__main__":
    unittest.main()

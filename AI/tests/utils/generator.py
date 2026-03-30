import os
import sys
import asyncio
import json
from pathlib import Path

# --- 프로젝트 루트 설정 ---
# ai_service 디렉토리 내의 app 모듈을 불러오기 위해 경로 추가
PROJECT_ROOT = Path(__file__).resolve().parent.parent.parent
AI_SERVICE_DIR = PROJECT_ROOT / "ai_service"
sys.path.append(str(AI_SERVICE_DIR))

from app.services.gms_engine import GMSGeminiClient

async def generate_legal_dataset(count: int = 20):
    """
    GMS API(Gemini)를 활용하여 부동산 법률 및 세무 관련 실전 테스트 질문셋을 생성합니다.
    """
    print(f"🔍 GMS API를 통해 {count}개의 테스트 데이터를 생성 중입니다...")
    
    client = GMSGeminiClient(model_name="gemini-2.0-flash")
    
    system_instruction = (
        "당신은 부동산 법률 및 세무 서비스의 품질 테스트를 위한 데이터 생성기입니다. "
        "사용자가 실제로 물어볼 법한 질문 20개를 생성하세요. "
        "질문은 '전세사기', '차용증 작성', '증여세', '임대차보호법' 등 다양한 주제를 포함해야 합니다. "
        "응답은 반드시 아래와 같은 JSON 배열 형식으로만 출력하세요.\n"
        '[{"message": "질문내용1", "history": []}, {"message": "질문내용2", "history": []}]'
    )
    
    prompt = f"부동산 및 세무 관련 실전 질문 {count}개를 JSON 배열 형식으로 생성해줘."
    
    try:
        response_text = await client.generate_response(system_instruction, prompt)
        
        # JSON 부분만 추출 (마크다운 백틱 제거 등)
        clean_json = response_text.replace("```json", "").replace("```", "").strip()
        dataset = json.loads(clean_json)
        
        # 결과 저장
        save_path = Path(__file__).parent / "test_queries.json"
        with open(save_path, "w", encoding="utf-8") as f:
            json.dump(dataset, f, ensure_ascii=False, indent=2)
            
        print(f"✅ 생성 완료: {save_path}")
        return dataset
        
    except Exception as e:
        print(f"❌ 데이터 생성 중 오류 발생: {str(e)}")
        return None

if __name__ == "__main__":
    # 비동기 실행 (테스트용)
    asyncio.run(generate_legal_dataset())

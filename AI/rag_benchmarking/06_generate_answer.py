import os
import chromadb
from sentence_transformers import SentenceTransformer
import requests
import json
import config

# 공유 서버 GPU 0번 사용 제한 규정 (임베딩 모델 로드 시 필요)
os.environ["CUDA_DEVICE_ORDER"] = "PCI_BUS_ID"
os.environ["CUDA_VISIBLE_DEVICES"] = "0"

def generate_rag_answer(user_query: str):
    """
    1. 사용자의 질문을 임베딩합니다.
    2. ChromaDB에서 가장 유사한 법률 조항 Top-3를 검색합니다.
    3. 검색된 조항들을 문맥(Context)으로 삼아 LLM에게 답변을 요구합니다.
    """
    print("1. 로컬 Chroma DB 연결...")
    client = chromadb.PersistentClient(path=config.CHROMA_DB_DIR)
    collection = client.get_collection(name=config.COLLECTION_NAME)

    print("2. 임베딩 모델 로드 중...")
    model = SentenceTransformer(config.EMBEDDING_MODEL_NAME)
    
    print(f"3. 질문 검색: '{user_query}'")
    query_embedding = model.encode([user_query], normalize_embeddings=True).tolist()
    
    # Top-3 조항 검색
    results = collection.query(
        query_embeddings=query_embedding,
        n_results=3
    )
    
    # 검색된 법률 조항 텍스트를 하나로 합침 (문맥 구성)
    contexts = []
    print("\n[참고한 법률 조항]")
    for i in range(len(results['documents'][0])):
        doc_content = results['documents'][0][i]
        doc_id = results['ids'][0][i]
        contexts.append(f"[{doc_id}] {doc_content}")
        print(f" - {doc_id}")
        
    context_text = "\n\n".join(contexts)

    # 4. LLM을 통한 답변 생성
    print("\n4. LLM을 통한 답변 생성 중...\n" + "="*50)
    
    system_prompt = """당신은 한국의 법률 및 금융 전문 AI 어시스턴트입니다.
반드시 아래 제공된 [법률 조항 문맥]만을 바탕으로 사용자의 [질문]에 답변하세요.
법률 조항에 없는 내용은 지어내지 말고 "제공된 문서에서 관련 내용을 찾을 수 없습니다."라고 답변하세요.
답변은 구체적이고 전문적으로 작성하며, 어느 부칙이나 조항을 근거로 했는지 명시하세요."""

    user_prompt = f"[법률 조항 문맥]\n{context_text}\n\n[질문]\n{user_query}"

    try:
        # config.py를 통해 .env에서 로드된 API 정보 가져오기
        GMS_KEY = config.GMS_API_KEY
        BASE_URL = config.GMS_BASE_URL
        
        if not GMS_KEY:
            print("\n[STOP] .env 파일에 GMS_KEY 가 설정되지 않았습니다!")
            print("프로젝트 최상단에 .env 파일을 만들고 GMS_KEY=본인키 를 입력해주세요.")
            return

        # 시스템 프롬프트와 유저 프롬프트를 Gemini 규격에 맞게 병합 
        # (혹은 시스템 인스트럭션 구조로도 가능하지만, RAG에서는 통문자로 넘기는 것이 제일 안전함)
        combined_prompt = f"{system_prompt}\n\n{user_prompt}"

        # GMS (Google Gemini) 네이티브 API 페이로드 규격
        headers = {
            "Content-Type": "application/json",
            "x-goog-api-key": GMS_KEY
        }
        
        payload = {
            "contents": [
                {
                    "parts": [
                        {"text": combined_prompt}
                    ]
                }
            ],
            "generationConfig": {
                "temperature": 0.0 # 사실성(Fact) 우선 모드
            }
        }
        
        # API 통신 실행
        response = requests.post(BASE_URL, headers=headers, json=payload)
        
        # HTTP 에러 시 상세 출력
        if response.status_code != 200:
            raise Exception(f"HTTP {response.status_code} - {response.text}")
            
        data = response.json()
        
        # Gemini JSON 구조에서 텍스트 결과값 파싱
        answer = data['candidates'][0]['content']['parts'][0]['text']
        
        print(f"\n🤖 [Gemini 2.0 Flash 답변]\n{answer}")
        print("="*80)
        
    except Exception as e:
        print("\n[❌ API 호출 요류 발생!]")
        print("GMS_KEY가 틀렸거나, 플랫폼 서버와 통신할 수 없습니다.")
        print(f"상세 에러 내역: {e}")

if __name__ == "__main__":
    # --- [매우 엄격한 실전 사용자(Edge Case) 테스트 셋] ---
    # 실제 사람들은 법률 용어를 쓰지 않고, 오타가 있으며, 자기 하소연을 섞어서 질문합니다.
    real_user_queries = [
        # 1. 일상어 기반 (법률 용어 부재)
        "친구가 급하다고 해서 5천만원 빌려줬는데 계속 안갚아 ㅠㅠ 이자라도 쳐서 받고 싶은데 법적으로 이자율 어디까지 됨? 최고 이자 얼마야?",
        
        # 2. 오타 및 띄어쓰기 파괴
        "미셩년잔데부모님모르게 아이패드 삿거든요 이거 다시환불대나여?",
        
        # 3. 사실관계와 조건이 구체적인 질의 (복합 추론 요구)
        "보증금이 2억이고 월세가 50인 원룸에 살고 있습니다. 계약 기간이 끝났는데 집주인이 돈을 안 돌려주면 당장 제가 할 수 있는 법적 조치가 뭐가 있나요? 내용증명? 임차권등기?",
        
        # 4. 없는 내용 (Hallucination 억제 테스트 - DB에 없는 내용을 유도)
        "2027년에 새로 개정된 우주항공법 제12조에서 화성 여행 시 보험금 규정이 어떻게 돼?"
    ]

    print("\n" + "#"*80)
    print("🚀 [실전 모의 RAG 테스트 시작] 모델: Gemini 2.0 Flash")
    print("#"*80)

    for idx, query in enumerate(real_user_queries, 1):
        print(f"\n\n👉 [Test Case {idx}] 사용자 입력: {query}")
        generate_rag_answer(query)

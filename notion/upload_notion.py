import json
import requests
import datetime
import sys

def main():
    config_path = r"C:\Users\SSAFY\.gemini\mcp_config.json"
    with open(config_path, 'r', encoding='utf-8') as f:
        config = json.load(f)
    
    token = config.get("mcpServers", {}).get("notion", {}).get("env", {}).get("NOTION_API_TOKEN", "")

    headers = {
        "Authorization": f"Bearer {token}",
        "Notion-Version": "2022-06-28",
        "Content-Type": "application/json"
    }

    # The ID of the parent page '정리'
    parent_page_id = "2955b192-1b6e-8060-88ff-e8f82dcc3be0"

    print("Uploading Retrospective to Notion...")
    
    # We will append block children to this page
    # Since '정리' seems to be a page, we use the append block children API
    blocks = [
        {
            "object": "block",
            "type": "heading_1",
            "heading_1": {
                "rich_text": [{"type": "text", "text": {"content": f"개발 회고 및 트러블슈팅 - {datetime.date.today().isoformat()}"}}]
            }
        },
        {
            "object": "block",
            "type": "heading_2",
            "heading_2": {
                "rich_text": [{"type": "text", "text": {"content": "1. 독소조항 탐지 및 챗봇 특화 (RAG 엔진 구축)"}}]
            }
        },
        {
            "object": "block",
            "type": "paragraph",
            "paragraph": {
                "rich_text": [{"type": "text", "text": {"content": "법률 문서(가맹계약서 등) 내 독소조항을 탐지하기 위해 BAAI/bge-m3 모델을 활용하여 임베딩을 구성하고 ChromaDB에 저장하는 RAG 파이프라인을 구축했습니다. 초기에 RAG 품질 문제가 있어, 프롬프트 엔지니어링과 함께 검색 결과를 컨텍스트로 전달하는 방식을 정교화하여 챗봇 응답의 정확도를 크게 향상시켰습니다."}}]
            }
        },
        {
            "object": "block",
            "type": "heading_2",
            "heading_2": {
                "rich_text": [{"type": "text", "text": {"content": "2. MLOps 파이프라인 도입 (DVC & WandB)"}}]
            }
        },
        {
            "object": "block",
            "type": "paragraph",
            "paragraph": {
                "rich_text": [{"type": "text", "text": {"content": "RAG 파이프라인의 성능 검증 및 지속적인 모델 개선을 위해 실험 관리가 필요해졌습니다. 이에 데이터 버저닝을 위해 DVC를, 실험/프롬프트 트래킹을 위해 Weights & Biases(WandB)를 도입하기로 결정했습니다. 또한 환경 설정의 유연성을 위해 pydantic-settings와 .env 방식을 적용하여 안전한 설정 관리를 시스템화했습니다."}}]
            }
        },
        {
            "object": "block",
            "type": "heading_2",
            "heading_2": {
                "rich_text": [{"type": "text", "text": {"content": "3. 프론트엔드 보안 모델 (Crypto - 자가 검증)"}}]
            }
        },
        {
            "object": "block",
            "type": "paragraph",
            "paragraph": {
                "rich_text": [{"type": "text", "text": {"content": "백엔드 통합 전, 프론트엔드 상에서 공개키/비밀키 쌍을 생성하고 데이터를 서명한 뒤 이를 로컬에서 검증하는 보안 자가 검증 로직을 구현했습니다. 이는 시스템의 신뢰성을 보장하기 위한 선제적 조치였습니다."}}]
            }
        },
        {
            "object": "block",
            "type": "heading_2",
            "heading_2": {
                "rich_text": [{"type": "text", "text": {"content": "4. UI/UX 전면 개편 (Glassmorphism)"}}]
            }
        },
        {
            "object": "block",
            "type": "paragraph",
            "paragraph": {
                "rich_text": [{"type": "text", "text": {"content": "기존 탭 방식에서 사이드바 내비게이션 구조로 전환하고, 네온 틸/민트 액센트와 글래스모피즘(Glassmorphism) 효과를 적용한 다크 테마로 사용자 편의성과 시각적 우수성을 확보했습니다."}}]
            }
        },
        {
            "object": "block",
            "type": "heading_2",
            "heading_2": {
                "rich_text": [{"type": "text", "text": {"content": "💡 주요 핵심 트러블슈팅: Notion API 연동"}}]
            }
        },
        {
            "object": "block",
            "type": "bulleted_list_item",
            "bulleted_list_item": {
                "rich_text": [{"type": "text", "text": {"content": "[문제] 개발 일지를 Notion에 자동 기록하려 했으나, MCP 웹 OAuth 팝업이 노출되지 않는 환경(CLI 등) 제약으로 연동 실패."}}]
            }
        },
        {
            "object": "block",
            "type": "bulleted_list_item",
            "bulleted_list_item": {
                "rich_text": [{"type": "text", "text": {"content": "[해결방안 모색] Notion 개발자 페이지에서 '내부 통합 API 토큰(Internal Integration)'을 직접 발급받음."}}]
            }
        },
        {
            "object": "block",
            "type": "bulleted_list_item",
            "bulleted_list_item": {
                "rich_text": [{"type": "text", "text": {"content": "[트러블슈팅] 토큰 발급 후 특정 페이지에 기록을 남기려 했으나 404 에러 발생. 페이지 UI 상에서 봇 초대 버튼이 보이지 않음."}}]
            }
        },
        {
            "object": "block",
            "type": "bulleted_list_item",
            "bulleted_list_item": {
                "rich_text": [{"type": "text", "text": {"content": "[최종 해결] API를 직접 호출하여 토큰이 권한을 가진 상위 루트 페이지('정리')의 고유 ID를 역추적하여 발췌한 다음, Append Block Children API를 사용해 성공적으로 내용을 밀어넣음."}}]
            }
        },
        {
            "object": "block",
            "type": "divider",
            "divider": {}
        }
    ]

    payload = {"children": blocks}
    
    res = requests.patch(f"https://api.notion.com/v1/blocks/{parent_page_id}/children", headers=headers, json=payload)
    
    if res.status_code == 200:
        print("Successfully uploaded retrospective to Notion!")
    else:
        print(f"Failed to upload: {res.status_code} - {res.text}")

if __name__ == "__main__":
    main()

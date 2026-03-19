import json
import requests
import os
from datetime import datetime

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

    # 1. '파이썬' 데이터베이스 ID 검색
    search_payload = {
        "query": "파이썬",
        "filter": {"value": "database", "property": "object"}
    }
    search_res = requests.post("https://api.notion.com/v1/search", headers=headers, json=search_payload)
    if search_res.status_code != 200:
        print(f"Search failed: {search_res.text}")
        return
    
    databases = search_res.json().get("results", [])
    if not databases:
        print("Database '파이썬' not found")
        return
    
    db_id = databases[0]["id"]
    print(f"Found Database ID: {db_id}")

    # 2. 데이터베이스 항목 생성
    payload = {
        "parent": {"database_id": db_id},
        "properties": {
            "이름": {
                "title": [{"text": {"content": "__pycache__란 무엇인가?"}}]
            },
            "상태": {
                "status": {"name": "시작 전"}
            },
            "유형": {
                "rich_text": [{"text": {"content": "Python 기초"}}]
            }
        },
        "children": [
            {
                "object": "block",
                "type": "heading_2",
                "heading_2": {
                    "rich_text": [{"text": {"content": "1. 정의"}}]
                }
            },
            {
                "object": "block",
                "type": "paragraph",
                "paragraph": {
                    "rich_text": [{"text": {"content": "파이썬 인터프리터가 소스 코드(.py)를 실행할 때 생성하는 바이트코드(.pyc) 캐시 디렉토리입니다."}}]
                }
            },
            {
                "object": "block",
                "type": "heading_2",
                "heading_2": {
                    "rich_text": [{"text": {"content": "2. 생성 이유"}}]
                }
            },
            {
                "object": "block",
                "type": "bulleted_list_item",
                "bulleted_list_item": {
                    "rich_text": [{"text": {"content": "실행 속도 향상: 매번 소스 코드를 해석하는 과정을 생략하고, 이미 컴파일된 바이트코드를 로드하여 프로그램의 시작 속도를 높입니다."}}]
                }
            },
            {
                "object": "block",
                "type": "bulleted_list_item",
                "bulleted_list_item": {
                    "rich_text": [{"text": {"content": "관리 효율성: Python 3.2+부터 도입되었으며, .pyc 파일들이 소스 디렉토리를 어지럽히지 않도록 하나의 폴더에서 관리합니다."}}]
                }
            },
            {
                "object": "block",
                "type": "heading_2",
                "heading_2": {
                    "rich_text": [{"text": {"content": "3. 주요 특징"}}]
                }
            },
            {
                "object": "block",
                "type": "paragraph",
                "paragraph": {
                    "rich_text": [{"text": {"content": "소스 코드가 수정되면 타임스탬프를 비교하여 자동으로 갱신됩니다. 안전하게 삭제할 수 있으며, 필요 시 파이썬이 자동으로 다시 생성합니다. 통상 .gitignore에 추가하여 버전 관리에서 제외하는 것이 관례입니다."}}]
                }
            }
        ]
    }

    res = requests.post("https://api.notion.com/v1/pages", headers=headers, json=payload)
    
    if res.status_code == 200:
        page_url = res.json().get("url")
        print(f"Successfully created Notion page: {page_url}")
    else:
        print(f"Failed to create page: {res.status_code} - {res.text}")

if __name__ == "__main__":
    main()

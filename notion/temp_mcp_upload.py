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

    # 1. Search for the DB
    search_payload = {
        "query": "트러블슈팅 및 개발일지 DB",
        "filter": {
            "value": "database",
            "property": "object"
        }
    }
    res_search = requests.post("https://api.notion.com/v1/search", headers=headers, json=search_payload)
    if res_search.status_code != 200:
        print("Search failed.")
        return
    results = res_search.json().get("results", [])
    if not results:
        print("Database not found.")
        return
    db_id = results[0]["id"]
    
    # 2. Add page to DB
    today_iso = datetime.date.today().isoformat()
    blocks = [
        {
            "object": "block",
            "type": "heading_2",
            "heading_2": {
                "rich_text": [{"type": "text", "text": {"content": "💡 증상"}}]
            }
        },
        {
            "object": "block",
            "type": "paragraph",
            "paragraph": {
                "rich_text": [{"type": "text", "text": {"content": "사용자가 Notion MCP 연동을 테스트하려고 하였으나, @makenotion/notion-mcp-server가 에디터 내부에서 자동으로 로드/인식되지 않거나, Notion API Integration 연결 시 특정 워크스페이스에 봇이 검색되지 않는 문제 발생."}}]
            }
        },
        {
            "object": "block",
            "type": "heading_2",
            "heading_2": {
                "rich_text": [{"type": "text", "text": {"content": "🔍 원인"}}]
            }
        },
        {
            "object": "block",
            "type": "paragraph",
            "paragraph": {
                "rich_text": [{"type": "text", "text": {"content": "1. MCP 공식 서버 도구가 초기화 과정에서 딜레이되거나 IDE 캐시 문제가 있었음.\n2. Notion UI에서 통합 계정을 만든 워크스페이스와 실제 기록하려는 워크스페이스('설탕' vs '정리')가 서로 달라서 권한 목록에 봇이 표시되지 않음."}}]
            }
        },
        {
            "object": "block",
            "type": "heading_2",
            "heading_2": {
                "rich_text": [{"type": "text", "text": {"content": "✅ 해결 방법 (우회 및 반영)"}}]
            }
        },
        {
            "object": "block",
            "type": "paragraph",
            "paragraph": {
                "rich_text": [{"type": "text", "text": {"content": "워스크페이스를 정확히 맞춰서 새로 Integration Token(RetroBot)을 생성하고, 페이지 공유를 통해 권한을 부여함. MCP가 네이티브 도구로 즉시 뜨지 않는 공백을 메우기 위해, 에디터(AI)가 자체적으로 파이썬 백그라운드 스크립트를 생성 및 실행(Agentic Automation)하여 마치 MCP 도구를 사용한 것과 동일한 UX 파이프라인으로 노션 트리(페이지 > DB > 글)를 완벽하게 구축해 냄."}}]
            }
        }
    ]

    item_payload = {
        "parent": { "database_id": db_id },
        "properties": {
            "제목": {
                "title": [
                    {
                        "text": {
                            "content": f"[Notion MCP] 연동 트러블슈팅 및 Agentic 자동화 해결 요약"
                        }
                    }
                ]
            },
            "날짜": {
                "date": {
                    "start": today_iso
                }
            },
            "카테고리": {
                "select": { "name": "트러블슈팅" }
            }
        },
        "children": blocks
    }
    
    res_item = requests.post("https://api.notion.com/v1/pages", headers=headers, json=item_payload)
    if res_item.status_code == 200:
        print("MCP Upload Success!")
    else:
        print(f"Upload Failed: {res_item.text}")

if __name__ == "__main__":
    main()

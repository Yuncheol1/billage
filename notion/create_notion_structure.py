import json
import requests
import datetime
import sys

def main():
    config_path = r"C:\Users\SSAFY\.gemini\mcp_config.json"
    try:
        with open(config_path, 'r', encoding='utf-8') as f:
            config = json.load(f)
        token = config.get("mcpServers", {}).get("notion", {}).get("env", {}).get("NOTION_API_TOKEN", "")
    except Exception as e:
        print(f"Error reading config: {e}")
        return

    headers = {
        "Authorization": f"Bearer {token}",
        "Notion-Version": "2022-06-28",
        "Content-Type": "application/json"
    }

    # The ID of the parent page '정리' which we have access to
    parent_page_id = "2955b192-1b6e-8060-88ff-e8f82dcc3be0"

    # 1. Create '빌리지' Page
    print("1. Creating '빌리지' Page...")
    page_payload = {
        "parent": { "type": "page_id", "page_id": parent_page_id },
        "properties": {
            "title": [
                {
                    "text": {
                        "content": "빌리지"
                    }
                }
            ]
        }
    }
    res_page = requests.post("https://api.notion.com/v1/pages", headers=headers, json=page_payload)
    if res_page.status_code != 200:
        print(f"Failed to create page: {res_page.text}")
        return
    village_page_id = res_page.json()["id"]
    print(f"  -> Created Page ID: {village_page_id}")

    # 2. Create Database inside '빌리지'
    print("\n2. Creating Database inside '빌리지'...")
    db_payload = {
        "parent": { "type": "page_id", "page_id": village_page_id },
        "title": [
            {
                "type": "text",
                "text": { "content": "트러블슈팅 및 개발일지 DB" }
            }
        ],
        "properties": {
            "제목": {
                "title": {}
            },
            "날짜": {
                "date": {}
            },
            "카테고리": {
                "select": {
                    "options": [
                        {"name": "트러블슈팅", "color": "red"},
                        {"name": "개발일지", "color": "blue"}
                    ]
                }
            }
        }
    }
    res_db = requests.post("https://api.notion.com/v1/databases", headers=headers, json=db_payload)
    if res_db.status_code != 200:
        print(f"Failed to create database: {res_db.text}")
        return
    db_id = res_db.json()["id"]
    print(f"  -> Created DB ID: {db_id}")

    # 3. Add Troubleshooting Retrospective to the Database
    print("\n3. Adding Retrospective to Database...")
    today_iso = datetime.date.today().isoformat()
    blocks = [
        {
            "object": "block",
            "type": "heading_2",
            "heading_2": {
                "rich_text": [{"type": "text", "text": {"content": "💡 노션 연동 트러블슈팅 요약"}}]
            }
        },
        {
            "object": "block",
            "type": "bulleted_list_item",
            "bulleted_list_item": {
                "rich_text": [{"type": "text", "text": {"content": "[문제점] Notion MCP를 통한 자동 기록을 시도했으나 브라우저 팝업/OAuth 문제 발생. 대안으로 발급받은 Internal Integration Token을 사용하려 했으나 특정 노션 페이지에서 봇이 검색되지 않아 공유 실패 밎 404 발생."}}]
            }
        },
        {
            "object": "block",
            "type": "bulleted_list_item",
            "bulleted_list_item": {
                "rich_text": [{"type": "text", "text": {"content": "[원인] 노션 UI 상 특정 워크스페이스 간 불일치 또는 앱 숨김 현상 작용."}}]
            }
        },
        {
            "object": "block",
            "type": "bulleted_list_item",
            "bulleted_list_item": {
                "rich_text": [{"type": "text", "text": {"content": "[해결책] UI를 우회하여, 직접 파이썬 스크립트를 작성 -> Search API로 해당 토큰이 접근할 수 있는 최상단 부모 페이지('정리')의 ID 색출 -> 직접 POST/PATCH 요청으로 노션 트리 구조(페이지 -> DB -> 페이지 -> 블록)를 생성."}}]
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
                            "content": f"[Notion API] MCP 연동 트러블슈팅 기록 ({today_iso})"
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
        print("\n🎉Successfully uploaded today's troubleshooting to the new database!")
    else:
        print(f"\nFailed to add page to database: {res_item.status_code} - {res_item.text}")

if __name__ == "__main__":
    main()

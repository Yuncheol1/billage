import json
import requests
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

    payload = {
        "query": "정리",
        "filter": {
            "value": "page",
            "property": "object"
        }
    }
    res = requests.post("https://api.notion.com/v1/search", headers=headers, json=payload)
    
    if res.status_code == 200:
        data = res.json()
        with open("notion_search_result.json", "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
        print("Search results saved to notion_search_result.json")
    else:
        print(f"Error {res.status_code}: {res.text}")

if __name__ == "__main__":
    main()

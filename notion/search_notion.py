import json
import requests
import os

def main():
    config_path = r"C:\Users\SSAFY\.gemini\mcp_config.json"
    if not os.path.exists(config_path):
        print(f"Config not found at {config_path}")
        return

    with open(config_path, 'r', encoding='utf-8') as f:
        config = json.load(f)
    
    token = config.get("mcpServers", {}).get("notion", {}).get("env", {}).get("NOTION_API_TOKEN", "")
    if not token:
        print("Token not found in config")
        return

    headers = {
        "Authorization": f"Bearer {token}",
        "Notion-Version": "2022-06-28",
        "Content-Type": "application/json"
    }

    # Search for the database with the name "파이썬"
    search_payload = {
        "query": "파이썬",
        "filter": {
            "value": "database",
            "property": "object"
        }
    }
    
    # print("Searching for databases...")
    res = requests.post("https://api.notion.com/v1/search", headers=headers, json=search_payload)
    
    if res.status_code == 200:
        data = res.json()
        results = data.get("results", [])
        if not results:
            print("No database named '파이썬' found. Searching for all objects...")
            res_all = requests.post("https://api.notion.com/v1/search", headers=headers, json={"query": ""})
            if res_all.status_code == 200:
                for obj in res_all.json().get("results", []):
                    title = "Unknown"
                    if obj["object"] == "database":
                        title = obj.get("title", [{}])[0].get("plain_text", "Untitled")
                    elif obj["object"] == "page":
                        props = obj.get("properties", {})
                        if "title" in props:
                            title = props["title"].get("title", [{}])[0].get("plain_text", "Untitled Page")
                        elif "Name" in props:
                             title = props["Name"].get("title", [{}])[0].get("plain_text", "Untitled Page")
                    
                    print(f"Found {obj['object']}: {title} (ID: {obj.get('id')})")
        else:
            for db in results:
                title = db.get("title", [{}])[0].get("plain_text", "Untitled")
                print(f"Found Database: {title} (ID: {db.get('id')})")
                print(f"Properties: {list(db.get('properties', {}).keys())}")
    else:
        print(f"Error {res.status_code}: {res.text}")

if __name__ == "__main__":
    main()

import json
import os

def get_test_dataset():
    """
    성능 테스트를 위한 시나리오별 데이터셋을 반환합니다.
    """
    return {
        "normal": [
            {"message": "전세 계약 시 확정일자는 왜 받아야 해?", "history": []},
            {"message": "차용증을 안 쓰면 나중에 돈을 못 받나?", "history": []},
            {"message": "임대인이 보증금을 안 돌려줄 때 대처법", "history": []}
        ],
        "long_context": [
            {
                "message": "아래 긴 법률 내용을 요약해주고 핵심만 알려줘.",
                "history": [
                    {"role": "user", "content": "주택임대차보호법 제1조... (매우 긴 텍스트 시뮬레이션)" * 50}
                ]
            }
        ],
        "error_case": [
            {"message": "", "history": []},  # 빈 메시지
            {"message": "A" * 5000, "history": []}  # 너무 긴 메시지
        ]
    }

def save_dataset_to_json(path="tests/performance/scripts/sample_data.json"):
    data = get_test_dataset()
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"✅ 테스트 데이터셋이 {path}에 저장되었습니다.")

if __name__ == "__main__":
    save_dataset_to_json()

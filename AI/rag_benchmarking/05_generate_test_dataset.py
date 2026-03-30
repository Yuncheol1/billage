import os
import json
import random
import re

OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "output")

def generate_rule_based_queries():
    print("1. 문서 로딩 중...")
    file_path = os.path.join(OUTPUT_DIR, "parsed_articles.json")
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"파싱된 파일이 없습니다: {file_path}")

    with open(file_path, "r", encoding='utf-8') as f:
        articles = json.load(f)
        
    print(f"총 {len(articles)}개의 조항 중에서 100개를 추출하여 쿼리를 생성합니다.")
    
    # 균등하게 100개의 조항 선택 (순서대로 적당히 간격을 띄워서 추출)
    step = max(1, len(articles) // 100)
    sampled_articles = articles[::step][:100]
    
    generated_dataset = []
    
    # 규칙 기반 질문 템플릿들
    # 1: 조항명(괄호 안)을 물어보는 패턴
    # 2: 조항 번호를 물어보는 패턴
    # 3: 특정 단어/조항의 개념을 물어보는 패턴
    templates_with_title = [
        "법률에서 '{title}'에 대한 내용은 무엇인가요?",
        "'{title}'과 관련된 규정에 대해 설명해주세요.",
        "법적으로 '{title}'은 어떻게 다루어지고 있나요?",
        "{title}에 대한 자세한 법적 근거가 궁금합니다.",
        "법에서 말하는 '{title}'의 구체적인 제약 사항이나 조건은 무엇일까요?"
    ]
    
    templates_with_article = [
        "{article_num}에 명시된 내용을 알고 싶습니다.",
        "{article_num}의 핵심적인 규정 내용은 어떻게 되나요?",
        "현행법상 {article_num}에서 규정하고 있는 바가 무엇인가요?",
        "법적으로 {article_num} 조항을 설명해 주시겠어요?",
        "{article_num}에 따르면 어떤 권리나 의무가 주어지나요?"
    ]

    print("\n2. 규칙 기반(Rule-based) Q&A 데이터 생성 (API 미사용)...")
    
    for idx, article in enumerate(sampled_articles, 1):
        source = article['source']
        article_id = article['article_id'] # 예: "제2조(신의성실)" 또는 "제9조"
        
        # 괄호 유무 및 내용 파싱 (정규표현식 사용)
        match = re.match(r"(제\d+조\w*)(\((.*?)\))?", article_id)
        
        query = ""
        if match:
            article_num = match.group(1) # 제2조
            title = match.group(3)       # 신의성실 (있을 경우)
            
            if title:
                # 제목이 있는 경우
                if random.choice([True, False]):
                    query = random.choice(templates_with_title).format(title=title)
                else:
                    query = random.choice(templates_with_article).format(article_num=article_id)
            else:
                # 제목이 없는 경우 (조항 번호만)
                query = random.choice(templates_with_article).format(article_num=article_num)
        else:
            # 매칭되지 않은 특수 형식 (서문 등)
            query = f"{article_id}에 대해 설명해 주세요."
            
        generated_dataset.append({
            "query": query,
            "target_source": source,
            "target_article_id": article_id
        })
        
    output_path = os.path.join(OUTPUT_DIR, "gold_dataset_100.json")
    with open(output_path, "w", encoding='utf-8') as f:
        json.dump(generated_dataset, f, ensure_ascii=False, indent=2)
        
    print(f"\n3. 총 {len(generated_dataset)}개의 샘플 쿼리가 API 없이 자동 생성되어 {output_path} 에 저장되었습니다.")

if __name__ == "__main__":
    generate_rule_based_queries()

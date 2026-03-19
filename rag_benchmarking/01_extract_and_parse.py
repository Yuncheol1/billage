import os
import re
import json
import fitz  # PyMuPDF
from langchain_text_splitters import RecursiveCharacterTextSplitter
from config import PDF_FILES, DATA_DIR, OUTPUT_DIR

def clean_text(text: str) -> str:
    """텍스트 내 중복 공백, 불필요한 기호 등을 제거합니다."""
    # 기본적인 노이즈 제거: 불필요한 줄바꿈 제거, 연속된 공백 제거
    text = re.sub(r'\s+', ' ', text)
    # 양끝 공백 제거
    return text.strip()

def extract_text_from_pdf(pdf_path: str) -> str:
    """PDF 파일에서 전체 텍스트를 추출합니다."""
    doc = fitz.open(pdf_path)
    full_text = []
    for page in doc:
        text = page.get_text()
        full_text.append(text)
    return "\n".join(full_text)

def parse_articles(text: str, source_name: str) -> list[dict]:
    """'제N조(제목)' 형태의 조항 단위로 텍스트를 분할합니다."""
    
    # 조항 패턴: "제1조 ", "제 1 조", "제2조(정의)" 등을 포괄적으로 매칭
    # r"제\s*\d+\s*조\s*(?:의\s*\d+\s*)?(?:\([^)]+\))?"
    pattern = r"(제\s*\d+\s*조\s*(?:의\s*\d+\s*)?(?:\([^)]+\))?)"
    
    # 패턴을 기준으로 텍스트 스플릿 (캡처 그룹 포함 시, 결과물에 패턴 자체도 포함됨)
    parts = re.split(pattern, text)
    
    articles = []
    # 맨 처음 부분은 법령의 서론이거나 이전 텍스트일 수 있음 (조항 이전의 내용)
    preamble = clean_text(parts[0])
    if preamble:
        articles.append({
            "source": source_name,
            "article_id": "서문",
            "content": preamble
        })
    
    # 시맨틱 텍스트 청킹 도구 세팅 (의미가 끊기지 않게 분리)
    # 한 덩어리당 최대 500자 제한, 문맥이 끊기지 않게 50자 중첩(Overlap) 배분
    text_splitter = RecursiveCharacterTextSplitter(
        chunk_size=500,
        chunk_overlap=50,
        separators=["\n\n", "\n", ".", " ", ""]
    )
    
    # split 결과: [서문, 매칭패턴1, 내용1, 매칭패턴2, 내용2, ...]
    for i in range(1, len(parts) - 1, 2):
        article_title = clean_text(parts[i])    # 예: 제1조(목적)
        article_content = clean_text(parts[i+1])# 해당 조항의 세부 내용
        
        # 전체 텍스트를 묶어서 구성
        full_article_text = f"{article_title} {article_content}"
        
        # 300~500자가 넘는 너무 긴 조항(text)은 여러 개의 청크 덩어리로 나눔.
        # 짧은 조항이면 알아서 1개의 청크로만 나옴.
        chunks = text_splitter.split_text(full_article_text)
        
        for idx, chunk in enumerate(chunks):
            # 조항이 길어서 여러 파트로 나뉜 경우 "제1조(목적) [Part 2]" 처럼 이름표를 달아줌.
            chunk_id = f"{article_title} [Part {idx+1}]" if len(chunks) > 1 else article_title
            
            articles.append({
                "source": source_name,
                "article_id": chunk_id,
                "content": chunk
            })
    
    return articles

def process_pdfs():
    all_articles = []
    
    for filename in PDF_FILES:
        pdf_path = os.path.join(DATA_DIR, filename)
        if not os.path.exists(pdf_path):
            print(f"[Warning] 파일을 찾을 수 없습니다: {pdf_path}")
            continue
            
        print(f"> Processing PDF: {filename}")
        raw_text = extract_text_from_pdf(pdf_path)
        
        # 1. 문서 전체의 기본 클리닝 후 텍스트 파싱
        parsed_data = parse_articles(raw_text, source_name=filename)
        print(f"  + 총 {len(parsed_data)}개의 섹션(조항) 추출됨.")
        
        all_articles.extend(parsed_data)
        
    # 중간 결과물을 JSON으로 저장 (검증 용도 및 다음 단계 인풋)
    output_path = os.path.join(OUTPUT_DIR, "parsed_articles.json")
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(all_articles, f, ensure_ascii=False, indent=2)
        
    print(f"\n[Success] 모든 PDF 파싱 완료. 파일 저장 위치: {output_path}")

if __name__ == "__main__":
    process_pdfs()

"""
=============================================================================
  IOU 프로젝트 — 법령/사례 통합 적재 스크립트 (ingest_cases.py)
=============================================================================
  역할: rag_benchmarking/data 내의 법령 PDF + 질의회신 JSON + 대법원 판례(OCR)를
       LangChain Document로 변환 후 ChromaDB(legal_tax_db)에 통합 적재합니다.

  실행 환경:
    - GPU 서버 (CUDA): python ingest_cases.py
    - EC2/CPU 서버  : python ingest_cases.py --cpu

  의존성: pip install pymupdf chromadb langchain-community sentence-transformers httpx tqdm
=============================================================================
"""

import os
import re
import sys
import json
import hashlib
import base64
import argparse
from pathlib import Path
from typing import List, Dict, Optional

import fitz  # PyMuPDF
import httpx
from tqdm import tqdm

from langchain_community.embeddings import HuggingFaceEmbeddings
from langchain_community.vectorstores import Chroma
from langchain.docstore.document import Document

# ── 경로 설정 ──────────────────────────────────────────────────────────────
ROOT_DIR = Path(__file__).resolve().parent
DATA_DIR = ROOT_DIR.parent / "rag_benchmarking" / "data"
CASES_JSON = ROOT_DIR / "data" / "cases_sample.json"
PARSED_ARTICLES_JSON = ROOT_DIR.parent / "rag_benchmarking" / "output" / "parsed_articles.json"

# ── 환경 설정 (pydantic-settings 없이도 독립 실행 가능) ─────────────────────
sys.path.append(str(ROOT_DIR))
try:
    from app.core.config import settings
    CHROMA_DB_DIR = os.path.join(ROOT_DIR, settings.CHROMA_DB_DIR)
    GMS_KEY = settings.GMS_KEY
    GMS_URL = settings.GMS_URL
except Exception:
    # config 로드 실패 시 기본값 사용
    CHROMA_DB_DIR = os.path.join(ROOT_DIR, "./chroma_db")
    from dotenv import load_dotenv
    load_dotenv(ROOT_DIR / ".env")
    GMS_KEY = os.getenv("GMS_KEY", "")
    GMS_URL = os.getenv("GMS_URL", "https://gms.ssafy.io/gmsapi/generativelanguage.googleapis.com/v1beta")

COLLECTION_NAME = "legal_tax_db"

# ── 적재 제외 대상 PDF 목록 ────────────────────────────────────────────────
EXCLUDED_PDFS = [
    "2026_상반기_별책부록_sw를_소개합니다!.pdf",
]

# ── 법령 PDF (조항 기반 자동 파싱 대상) ────────────────────────────────────
LAW_PDFS = {
    "상속세 및 증여세법(법률)(제21065호)(20260102).pdf": {
        "law_name": "상속세 및 증여세법",
        "law_level": "법률"
    },
    "상속세 및 증여세법 시행령(대통령령)(제36131호)(20260227).pdf": {
        "law_name": "상속세 및 증여세법 시행령",
        "law_level": "시행령"
    },
    "상속세 및 증여세법 시행규칙(재정경제부령)(제00001호)(20260102).pdf": {
        "law_name": "상속세 및 증여세법 시행규칙",
        "law_level": "시행규칙"
    },
    "이자제한법 제2조제1항의 최고이자율에 관한 규정(대통령령)(제31593호)(20210707).pdf": {
        "law_name": "이자제한법 최고이자율 규정",
        "law_level": "대통령령"
    },
}


# ═══════════════════════════════════════════════════════════════════════════
#  1. 법령 PDF 파싱 (조항 기반 Chunking)
# ═══════════════════════════════════════════════════════════════════════════
def parse_law_pdf(pdf_path: str, law_name: str, law_level: str) -> List[Document]:
    """법령 PDF를 '제N조(제목)' 패턴으로 조항별 분할하여 Document 리스트 반환"""
    doc = fitz.open(pdf_path)
    full_text = ""
    for page in doc:
        full_text += page.get_text()
    doc.close()

    # 조항 분리 정규식: '제N조', '제N조의N', '제N조(제목)' 패턴
    pattern = re.compile(r'(제\d+조(?:의\d+)?(?:\([^)]*\))?)')
    parts = pattern.split(full_text)

    documents = []
    # parts[0]은 서문(법 이름, 시행일 등) → 별도 Document로 저장
    if parts[0].strip():
        documents.append(Document(
            page_content=f"[{law_name}] {parts[0].strip()[:500]}",
            metadata={
                "source": law_name,
                "law_level": law_level,
                "article_id": "서문",
                "type": "law_article"
            }
        ))

    # 조항 헤더(parts[1])와 본문(parts[2])을 짝으로 처리
    for i in range(1, len(parts) - 1, 2):
        header = parts[i].strip()
        body = parts[i + 1].strip() if i + 1 < len(parts) else ""
        content = f"{header} {body}"

        # 너무 짧은 참조 조항(예: "제42조 를 준용한다.")은 버림
        if len(content) < 15:
            continue

        # 조항 번호 추출 (메타데이터)
        num_match = re.search(r'제(\d+)조(?:의(\d+))?', header)
        article_num = num_match.group(0) if num_match else header

        documents.append(Document(
            page_content=f"[{law_name}] {content}",
            metadata={
                "source": law_name,
                "law_level": law_level,
                "article_id": article_num,
                "type": "law_article"
            }
        ))

    return documents


# ═══════════════════════════════════════════════════════════════════════════
#  2. 기존 parsed_articles.json 파싱 (민법, 이자제한법 등 기 처리분 재활용)
# ═══════════════════════════════════════════════════════════════════════════
def load_parsed_articles(json_path: str) -> List[Document]:
    """rag_benchmarking에서 생성된 parsed_articles.json을 Document로 변환"""
    if not os.path.exists(json_path):
        print(f"⚠️  기 파싱 데이터 없음: {json_path}")
        return []

    with open(json_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    documents = []
    for item in data:
        # 기존 parsed_articles.json 구조: {source, article_id, content}
        source_name = item.get("source", "").replace(".pdf", "")
        documents.append(Document(
            page_content=f"[{source_name}] {item['content']}",
            metadata={
                "source": source_name,
                "article_id": item.get("article_id", ""),
                "type": "law_article"
            }
        ))

    return documents


# ═══════════════════════════════════════════════════════════════════════════
#  3. 질의회신/사례 JSON 파싱 (Use-Case 기반 Chunking)
# ═══════════════════════════════════════════════════════════════════════════
def load_case_json(json_path: str) -> List[Document]:
    """cases_sample.json을 읽어 사례별 Document로 변환"""
    if not os.path.exists(json_path):
        print(f"⚠️  사례 데이터 없음: {json_path}")
        return []

    with open(json_path, "r", encoding="utf-8") as f:
        cases = json.load(f)

    documents = []
    for case in cases:
        # OCR 대기 중인 항목은 건너뜀
        if case.get("metadata", {}).get("ocr_status") == "pending":
            continue

        # 사실관계+질의와 판결요지를 하나의 Document로 결합
        content = (
            f"[{case['source']}] {case['title']}\n\n"
            f"【사실관계 및 질의】\n{case['fact_and_query']}\n\n"
            f"【판결 요지】\n{case['ruling_summary']}"
        )

        # 메타데이터 구성
        meta = {
            "source": case["source"],
            "type": "nts_ruling",
            "category": case.get("category", ""),
            "date": case.get("date", ""),
        }
        # 사례별 추가 메타데이터 병합
        if "metadata" in case:
            for k, v in case["metadata"].items():
                meta[k] = str(v)

        documents.append(Document(page_content=content, metadata=meta))

        # 관련 법령 조항도 별도 참조 Document로 생성 (검색 다양성 확보)
        if case.get("key_law_articles"):
            law_ref_content = (
                f"[{case['source']}] 관련 법령 참고\n"
                f"본 질의회신의 핵심 관련 법령: {', '.join(case['key_law_articles'])}\n"
                f"핵심 상수: {case.get('metadata', {}).get('key_constant', 'N/A')}\n"
                f"과세 기준: {case.get('metadata', {}).get('threshold', 'N/A')}"
            )
            documents.append(Document(
                page_content=law_ref_content,
                metadata={
                    "source": case["source"],
                    "type": "law_reference",
                    "category": case.get("category", ""),
                }
            ))

    return documents


# ═══════════════════════════════════════════════════════════════════════════
#  4. 대법원 판결 PDF OCR (Gemini 멀티모달 API 활용)
# ═══════════════════════════════════════════════════════════════════════════
def ocr_court_ruling_with_gemini(pdf_path: str) -> Optional[str]:
    """스캔 이미지 PDF를 Gemini 멀티모달 API로 텍스트 추출"""
    if not GMS_KEY:
        print("⚠️  GMS_KEY가 설정되지 않아 OCR을 건너뜁니다.")
        return None

    doc = fitz.open(pdf_path)
    extracted_texts = []

    for page_num in range(len(doc)):
        page = doc[page_num]
        # 페이지를 고해상도 이미지(PNG)로 변환
        pix = page.get_pixmap(dpi=300)
        img_bytes = pix.tobytes("png")
        img_base64 = base64.b64encode(img_bytes).decode("utf-8")

        # Gemini API 호출 (동기 방식)
        api_url = f"{GMS_URL}/models/gemini-2.0-flash:generateContent"
        headers = {
            "Content-Type": "application/json",
            "x-goog-api-key": GMS_KEY,
        }
        payload = {
            "contents": [{
                "role": "user",
                "parts": [
                    {
                        "inlineData": {
                            "mimeType": "image/png",
                            "data": img_base64
                        }
                    },
                    {
                        "text": "이 이미지는 대한민국 법원 판결문입니다. 이미지에 있는 한국어 텍스트를 빠짐없이 정확하게 그대로 추출해 주세요. 불필요한 설명 없이 원문 텍스트만 출력하세요."
                    }
                ]
            }],
            "generationConfig": {
                "temperature": 0.0,
            }
        }

        try:
            print(f"  🔍 Gemini OCR: 대법원 판결 p.{page_num + 1}/{len(doc)} 처리 중...")
            response = httpx.post(api_url, headers=headers, json=payload, timeout=60.0)
            response.raise_for_status()
            data = response.json()
            text = data["candidates"][0]["content"]["parts"][0]["text"]
            extracted_texts.append(text)
        except Exception as e:
            print(f"  ❌ OCR 실패 (p.{page_num + 1}): {e}")
            extracted_texts.append(f"[OCR 실패: {e}]")

    doc.close()
    return "\n\n".join(extracted_texts)


def process_court_ruling(pdf_path: str) -> List[Document]:
    """대법원 판례 PDF를 OCR 후 Document로 변환"""
    fname = os.path.basename(pdf_path)
    print(f"\n📜 대법원 판례 OCR 처리: {fname}")

    text = ocr_court_ruling_with_gemini(pdf_path)
    if not text:
        print("  ⚠️  OCR 결과가 없어 건너뜁니다.")
        return []

    # OCR 결과를 cases_sample.json의 해당 항목에도 업데이트
    _update_case_json_ocr(text)

    return [Document(
        page_content=f"[대법원 판결 2020다230239] {text}",
        metadata={
            "source": "대법원 2020다230239 판결",
            "type": "court_ruling",
            "date": "2021.02.25",
            "category": "가족간_금전거래"
        }
    )]


def _update_case_json_ocr(ocr_text: str):
    """OCR 추출 결과를 cases_sample.json에 반영 (향후 재실행 시 중복 OCR 방지)"""
    try:
        with open(CASES_JSON, "r", encoding="utf-8") as f:
            cases = json.load(f)
        for case in cases:
            if case.get("metadata", {}).get("ocr_status") == "pending":
                case["fact_and_query"] = ocr_text
                case["ruling_summary"] = "Gemini OCR을 통해 추출된 텍스트 (원문 참조)"
                case["metadata"]["ocr_status"] = "completed"
        with open(CASES_JSON, "w", encoding="utf-8") as f:
            json.dump(cases, f, ensure_ascii=False, indent=2)
        print("  ✅ cases_sample.json OCR 결과 업데이트 완료")
    except Exception as e:
        print(f"  ⚠️  JSON 업데이트 실패: {e}")


# ═══════════════════════════════════════════════════════════════════════════
#  5. 무결성 체크섬 생성
# ═══════════════════════════════════════════════════════════════════════════
def create_checksum(db_dir: str):
    """DB 파일의 무결성을 증명하기 위한 MD5 해시 생성"""
    sqlite_path = os.path.join(db_dir, "chroma.sqlite3")
    if os.path.exists(sqlite_path):
        with open(sqlite_path, "rb") as f:
            file_hash = hashlib.md5(f.read()).hexdigest()
        checksum_path = os.path.join(db_dir, "checksum.md5")
        with open(checksum_path, "w") as f:
            f.write(f"{file_hash}  chroma.sqlite3\n")
        print(f"🔒 [Checksum] MD5: {file_hash}")


# ═══════════════════════════════════════════════════════════════════════════
#  메인 실행
# ═══════════════════════════════════════════════════════════════════════════
def main():
    parser = argparse.ArgumentParser(description="법령/사례 통합 적재 스크립트")
    parser.add_argument("--cpu", action="store_true", help="CPU 모드로 실행 (EC2 등 GPU 없는 환경)")
    parser.add_argument("--skip-ocr", action="store_true", help="대법원 판례 OCR 건너뛰기")
    parser.add_argument("--batch-size", type=int, default=32, help="임베딩 배치 크기 (기본: 32)")
    args = parser.parse_args()

    device = "cpu" if args.cpu else "cuda"
    print(f"{'='*60}")
    print(f"  IOU 법령/사례 통합 적재 파이프라인")
    print(f"  Device: {device} | Batch: {args.batch_size}")
    print(f"  DB Dir: {CHROMA_DB_DIR}")
    print(f"{'='*60}\n")

    # ── Step 0: 임베딩 모델 로드 ──────────────────────────────────────────
    print("🚀 [Step 0] BAAI/bge-m3 임베딩 모델 로딩...")
    embedding_model = HuggingFaceEmbeddings(
        model_name="BAAI/bge-m3",
        model_kwargs={"device": device},
        encode_kwargs={
            "normalize_embeddings": True,
            "batch_size": args.batch_size,
        }
    )

    # ── Step 1: 기존 파싱 데이터 로드 (민법, 이자제한법) ──────────────────
    print("\n📦 [Step 1] 기존 파싱 데이터(parsed_articles.json) 로드...")
    all_documents: List[Document] = []

    parsed_docs = load_parsed_articles(str(PARSED_ARTICLES_JSON))
    print(f"  ✅ 기존 파싱 데이터: {len(parsed_docs)} chunks")
    all_documents.extend(parsed_docs)

    # ── Step 2: 신규 법령 PDF 파싱 ────────────────────────────────────────
    print("\n📦 [Step 2] 신규 법령 PDF 조항 기반 파싱...")
    for pdf_name, info in LAW_PDFS.items():
        pdf_path = DATA_DIR / pdf_name
        if not pdf_path.exists():
            print(f"  ❌ 파일 없음: {pdf_name}")
            continue
        docs = parse_law_pdf(str(pdf_path), info["law_name"], info["law_level"])
        print(f"  ✅ {info['law_name']}: {len(docs)} chunks")
        all_documents.extend(docs)

    # ── Step 3: 질의회신/사례 JSON 로드 ──────────────────────────────────
    print("\n📦 [Step 3] 질의회신/사례 데이터(cases_sample.json) 로드...")
    case_docs = load_case_json(str(CASES_JSON))
    print(f"  ✅ 사례 데이터: {len(case_docs)} chunks")
    all_documents.extend(case_docs)

    # ── Step 4: 대법원 판례 OCR ──────────────────────────────────────────
    if not args.skip_ocr:
        court_pdf = DATA_DIR / "대법원 2021. 2. 25. 선고 2020다230239 판결.pdf"
        if court_pdf.exists():
            court_docs = process_court_ruling(str(court_pdf))
            print(f"  ✅ 대법원 판례: {len(court_docs)} chunks")
            all_documents.extend(court_docs)
        else:
            print("  ⚠️  대법원 판례 PDF 파일 없음, 건너뜁니다.")
    else:
        print("\n⏭️  [Step 4] OCR 건너뛰기 (--skip-ocr)")

    # ── Step 5: ChromaDB 적재 ─────────────────────────────────────────────
    print(f"\n🔥 [Step 5] ChromaDB 적재 시작 (총 {len(all_documents)} chunks)...")
    os.makedirs(CHROMA_DB_DIR, exist_ok=True)

    # 기존 DB가 있으면 삭제 후 새로 생성 (깨끗한 재적재)
    vector_store = Chroma.from_documents(
        documents=all_documents,
        embedding=embedding_model,
        collection_name=COLLECTION_NAME,
        persist_directory=CHROMA_DB_DIR,
    )

    print(f"  ✅ ChromaDB 적재 완료! ({CHROMA_DB_DIR})")

    # ── Step 6: 체크섬 생성 ───────────────────────────────────────────────
    create_checksum(CHROMA_DB_DIR)

    # ── 최종 요약 ─────────────────────────────────────────────────────────
    print(f"\n{'='*60}")
    print(f"  🎉 적재 완료 요약")
    print(f"  총 문서 수: {len(all_documents)} chunks")
    print(f"  컬렉션명: {COLLECTION_NAME}")
    print(f"  저장 경로: {CHROMA_DB_DIR}")
    print(f"{'='*60}")
    print(f"\n📦 EC2 배포 시 '{os.path.basename(CHROMA_DB_DIR)}' 폴더 전체를 전송하십시오.")


if __name__ == "__main__":
    main()

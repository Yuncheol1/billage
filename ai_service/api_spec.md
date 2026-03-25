# AI Microservice API Specification (v2.0)

> ERD 기반 재설계 — Spring이 보유한 실제 DB 스키마(`contract`, `contract_clause`, `tax_calculation` 등)에 정확히 매핑되도록 요청/응답을 재정의하였습니다.

---

## 🔐 공통 보안 사항 (Authentication)

모든 요청의 HTTP Header에 아래 인증 키를 포함해야 합니다.

| Header Name | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `Content-Type` | `string` | **Yes** | `application/json` |
| `X-Internal-API-Key` | `string` | **Yes** | 서버 간 인증 키 |

> 키가 누락되거나 불일치 시 → `403 Forbidden`

---

## 1. 차용증 독소조항 및 세금 분석

차용증 전문 또는 특정 조항을 분석하여 독소조항과 세금 리스크를 판단합니다.

- **Method:** `POST`
- **Path:** `/internal/v1/analyze`

#### [Request Body]

| Field | Type | Required | Default | 매핑 DB 컬럼 | Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `contract_seq` | `int` | No | `null` | `contract.contract_seq` | 계약 식별자 (추적/로깅용) |
| `contract_text` | `string` | **Yes** | - | - | 차용증 전체 또는 부분 텍스트 (Spring이 조합하여 전달) |
| `mode` | `string` | No | `"full"` | - | `"full"` (전체 문서) / `"partial"` (조항 단위) |
| `template_type` | `string` | No | `null` | `contract.template_type` | `FRIEND_SMALL` / `FAMILY_JEONSE` / `PARTNER_BIZ` / `COLLEAGUE_EMERGENCY` |
| `principal_amount` | `decimal` | No | `null` | `contract.principal_amount` | 대여 원금 |
| `interest_rate` | `decimal` | No | `null` | `contract.interest_rate` | 연 이자율 (0.046 = 4.6%) |
| `repayment_method` | `string` | No | `null` | `contract.repayment_method` | `BULLET` / `INSTALLMENT` / `CUSTOM` |
| `start_dt` | `string` | No | `null` | `contract.start_dt` | 대출 시작일 (`YYYY-MM-DD`) |
| `end_dt` | `string` | No | `null` | `contract.end_dt` | 대출 종료일 (`YYYY-MM-DD`) |
| `clauses` | `array` | No | `[]` | `contract_clause` | 개별 조항 리스트 (아래 참조) |

**clauses 배열 내부 객체:**

| Field | Type | Required | 매핑 DB 컬럼 | Description |
| :--- | :--- | :--- | :--- | :--- |
| `clause_id` | `string` | No | `contract_clause.clause_id` | `INTEREST` / `REPAYMENT_DATE` / `PENALTY` |
| `title` | `string` | No | `contract_clause.title` | 조항 제목 |
| `content` | `string` | **Yes** | `contract_clause.content` | 조항 본문 |

#### [Request Example]
```json
{
  "contract_seq": 42,
  "contract_text": "제1조(대여금) 금 10,000,000원... 제3조(특약) 상환 불이행 시 신체를 포기한다.",
  "mode": "full",
  "template_type": "FAMILY_JEONSE",
  "principal_amount": 10000000,
  "interest_rate": 0.0,
  "repayment_method": "BULLET",
  "start_dt": "2026-03-17",
  "end_dt": "2027-03-17",
  "clauses": [
    { "clause_id": "PENALTY", "title": "특약사항", "content": "상환 불이행 시 신체를 포기한다." }
  ]
}
```

#### [Response Body]

| Field | Type | Description |
| :--- | :--- | :--- |
| `is_safe` | `bool` | 전체적으로 서명해도 안전한지 여부 |
| `warnings` | `array` | 탐지된 위험 항목 리스트 |
| `warnings[].risk_level` | `string` | `HIGH` / `MEDIUM` / `LOW` |
| `warnings[].category` | `string` | `TOXIC_CLAUSE` / `TAX_ISSUE` / `MISSING_INFO` |
| `warnings[].clause_id` | `string` | 문제가 된 조항 식별자 (매핑용) |
| `warnings[].reason` | `string` | 판례/세법에 근거한 위험 사유 |
| `warnings[].suggestion` | `string` | 수정 권고안 |
| `tax_summary` | `object` | 세금 관련 요약 정보 (선택적 응답) |
| `tax_summary.gift_tax_risk` | `bool` | 증여세 추징 위험 여부 |
| `tax_summary.estimated_gift_tax` | `decimal` | 추정 증여세액 |
| `tax_summary.withholding_tax_rate` | `string` | 원천징수 세율 안내 (예: `"27.5%"`) |
| `summary` | `string` | 분석 결과 총평 |

#### [Response Example]
```json
{
  "is_safe": false,
  "warnings": [
    {
      "risk_level": "HIGH",
      "category": "TOXIC_CLAUSE",
      "clause_id": "PENALTY",
      "reason": "신체 포기 조항은 민법 제103조에 의거하여 무효입니다.",
      "suggestion": "해당 조항을 삭제하거나 법적으로 유효한 담보 조항으로 대체하세요."
    },
    {
      "risk_level": "HIGH",
      "category": "TAX_ISSUE",
      "clause_id": null,
      "reason": "가족 간(FAMILY) 무이자 대여는 증여세 부과 대상입니다. (적정이자율 연 4.6% 기준 미달)",
      "suggestion": "이자율을 연 4.6% 이상으로 설정하고 이자 지급 내역을 금융기록으로 보존하세요."
    }
  ],
  "tax_summary": {
    "gift_tax_risk": true,
    "estimated_gift_tax": 460000,
    "withholding_tax_rate": "27.5%"
  },
  "summary": "독소조항 1건, 증여세 위험 1건이 발견되었습니다. 서명 전 수정이 반드시 필요합니다."
}
```

---

## 2. 자연어 기반 특약사항 생성

사용자의 자연어 의도를 법적으로 유효한 `contract_clause` 레코드 형태로 변환합니다.

- **Method:** `POST`
- **Path:** `/internal/v1/clauses/generate`

#### [Request Body]

| Field | Type | Required | Default | 매핑 DB 컬럼 | Description |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `contract_seq` | `int` | No | `null` | `contract.contract_seq` | 소속 계약 식별자 |
| `user_intent` | `string` | **Yes** | - | - | 자연어 의도 (예: `"돈 안 갚으면 차 뺏을 거야"`) |
| `contract_context` | `string` | No | `""` | - | 현재까지 작성된 차용증 문맥 |
| `template_type` | `string` | No | `null` | `contract.template_type` | 계약 유형 (맥락 보강용) |

#### [Request Example]
```json
{
  "contract_seq": 42,
  "user_intent": "돈 안 갚으면 타고 다니는 차 뺏을 거야",
  "contract_context": "채권자 홍길동, 채무자 김철수, 원금 1000만원",
  "template_type": "FRIEND_SMALL"
}
```

#### [Response Body]

| Field | Type | 매핑 DB 컬럼 | Description |
| :--- | :--- | :--- | :--- |
| `clause_id` | `string` | `contract_clause.clause_id` | 생성된 조항의 유형 식별자 |
| `title` | `string` | `contract_clause.title` | 조항 제목 |
| `legal_clause` | `string` | `contract_clause.content` | 법적으로 다듬어진 특약사항 본문 |
| `explanation` | `string` | - | 해당 특약의 법적 효력 설명 |

#### [Response Example]
```json
{
  "clause_id": "COLLATERAL_VEHICLE",
  "title": "차량 양도담보 설정",
  "legal_clause": "채무자가 상환 기일 내에 원리금을 변제하지 아니할 경우, 채무자 소유의 차량에 대하여 양도담보권을 설정한다.",
  "explanation": "이 조항은 채무 불이행 시 특정 자산을 담보로 확보하는 양도담보 설정의 효력을 가집니다."
}
```

---

## 3. 채팅 내역 기반 특약사항 생성

두 사용자(채권자/채무자)의 채팅 대화 내역을 분석하여 합의된 조건을 법적 특약사항으로 변환합니다.
Spring이 MongoDB에서 조회·가공한 메시지 배열을 전달합니다.

- **Method:** `POST`
- **Path:** `/internal/v1/clauses/from-chat`

#### [Request Body]

| Field | Type | Required | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `contract_seq` | `int` | No | `null` | 소속 계약 식별자 |
| `room_id` | `string` | No | `null` | 채팅방 ID (추적/로깅용) |
| `template_type` | `string` | No | `null` | 계약 유형 (`FRIEND_SMALL` 등) |
| `contract_context` | `string` | No | `""` | 현재까지 작성된 차용증 문맥 |
| `chat_messages` | `array` | **Yes** | - | 두 사용자 간 채팅 메시지 배열 |

**chat_messages 배열 내부 객체:**

| Field | Type | Required | Description |
| :--- | :--- | :--- | :--- |
| `sender_seq` | `int` | **Yes** | 발신자 사용자 식별자 |
| `content` | `string` | **Yes** | 메시지 내용 |
| `created_at` | `string` | No | 메시지 전송 시간 (ISO 8601) |
| `msg_type` | `string` | No | `TEXT` / `AI_PROPOSAL` 등 |

#### [Request Example]
```json
{
  "contract_seq": 42,
  "room_id": "00161bfa-1f2b-4096-b2ed-019051301fc9",
  "template_type": "FRIEND_SMALL",
  "contract_context": "채권자 홍길동, 채무자 김철수, 원금 1000만원",
  "chat_messages": [
    { "sender_seq": 105, "content": "이자는 어떻게 할까?", "msg_type": "TEXT" },
    { "sender_seq": 106, "content": "무이자로 하자", "msg_type": "TEXT" },
    { "sender_seq": 105, "content": "돈 안 갚으면 네 차 압류하는 걸로", "msg_type": "TEXT" },
    { "sender_seq": 106, "content": "알겠어", "msg_type": "TEXT" }
  ]
}
```

#### [Response Body]

| Field | Type | Description |
| :--- | :--- | :--- |
| `clauses` | `array` | 생성된 특약사항 배열 |
| `clauses[].clause_id` | `string` | 조항 유형 식별자 (`contract_clause.clause_id`) |
| `clauses[].title` | `string` | 조항 제목 (`contract_clause.title`) |
| `clauses[].legal_clause` | `string` | 법적으로 정제된 특약 본문 (`contract_clause.content`) |
| `clauses[].explanation` | `string` | 해당 조항의 법적 의미 설명 |
| `summary` | `string` | 대화 분석 요약 |

#### [Response Example]
```json
{
  "clauses": [
    {
      "clause_id": "COLLATERAL_VEHICLE",
      "title": "차량 양도담보 설정",
      "legal_clause": "채무자가 상환 기일 내에 원리금을 변제하지 아니할 경우, 채무자 소유의 차량에 대하여 양도담보권을 설정한다.",
      "explanation": "채무 불이행 시 차량을 담보로 확보하는 양도담보 조항입니다."
    },
    {
      "clause_id": "INTEREST",
      "title": "무이자 약정",
      "legal_clause": "본 계약의 대여금에 대하여 이자를 부과하지 아니한다.",
      "explanation": "무이자 합의 조항입니다. 단, 가족 간 거래 시 증여세 이슈가 발생할 수 있습니다."
    }
  ],
  "summary": "대화에서 '무이자 합의'와 '채무 불이행 시 차량 담보 설정' 2건의 특약 의도가 파악되었습니다."
}
```

---

## 4. 세무/법률 Q&A 챗봇

판례 및 세법 DB(RAG)를 기반으로 전문적인 답변을 제공합니다.

- **Method:** `POST`
- **Path:** `/internal/v1/chat`

#### [Request Body]

| Field | Type | Required | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| `message` | `string` | **Yes** | - | 사용자의 현재 질문 |
| `history` | `array` | No | `[]` | 이전 대화 내역 (`role`, `content` 객체 배열) |
| `contract_seq` | `int` | No | `null` | 특정 계약 맥락의 질문일 때 전달 |

**history 배열 내부 객체:**

| Field | Type | Description |
| :--- | :--- | :--- |
| `role` | `string` | `"user"` 또는 `"assistant"` |
| `content` | `string` | 메시지 내용 |

#### [Request Example]
```json
{
  "message": "부모님께 5천만원 빌릴 때 증여세 안 나오게 하려면?",
  "history": [],
  "contract_seq": null
}
```

#### [Response Body]

| Field | Type | Description |
| :--- | :--- | :--- |
| `reply` | `string` | RAG 기반 전문 답변 |

#### [Response Example]
```json
{
  "reply": "부모 자식 간 거래는 원칙적으로 '증여'로 추정됩니다. 증여세를 피하려면: 1) 반드시 차용증을 작성하고 2) 적정 이자율(연 4.6%) 이상 적용 3) 매달 이자 지급 내역을 계좌이체로 보존해야 합니다."
}
```

---

## 🚦 공통 에러 코드

| Status | Code | Description |
| :--- | :--- | :--- |
| `200` | `OK` | 성공 |
| `400` | `Bad Request` | 필수 필드 누락 또는 형식 오류 |
| `403` | `Forbidden` | `X-Internal-API-Key` 불일치 |
| `422` | `Unprocessable Entity` | Pydantic 검증 실패 (타입 불일치 등) |
| `500` | `Internal Server Error` | AI 엔진 또는 GMS Gateway 장애 |

---

## 📌 ERD 데이터 흐름 요약

```
[Spring] contract + contract_clause 테이블 조회
   ↓ JSON 조합
[FastAPI] /internal/v1/analyze ← 분석 요청
   ↓ RAG + Gemini
[FastAPI] warnings[] + tax_summary 응답
   ↓
[Spring] tax_calculation 테이블에 결과 저장
         contract.status 업데이트 (DRAFT → NEGOTIATING)
```

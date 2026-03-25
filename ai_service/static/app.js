const API_BASE = "/internal/v1";

function getApiKey() {
    return document.getElementById("apiKey").value;
}

// 1. 차용증 독소조항 / 세금 분석
async function testAnalyze() {
    const resultBox = document.getElementById("analyzeResult");
    resultBox.innerText = "분석 중... (AI 응답 대기)";

    // 조항(clauses) 수집
    const clauseRows = document.querySelectorAll("#analyzeClauseList .analyze-clause-row");
    const clauses = [];
    clauseRows.forEach(row => {
        const clauseId = row.querySelector(".clause-id-select").value;
        const title = row.querySelector(".clause-title-input").value.trim();
        const content = row.querySelector(".clause-content-input").value.trim();
        if (content) {
            clauses.push({ clause_id: clauseId, title: title, content: content });
        }
    });

    const contractSeq = document.getElementById("analyzeContractSeq").value;
    const templateType = document.getElementById("analyzeTemplateType").value;

    const payload = {
        contract_text: document.getElementById("analyzeText").value,
        mode: document.getElementById("analyzeMode").value,
        principal_amount: parseInt(document.getElementById("principal").value) || 0,
        interest_rate: parseFloat(document.getElementById("interest").value) / 100 || 0,
        repayment_method: document.getElementById("analyzeRepayment").value,
        start_dt: document.getElementById("analyzeStartDt").value || null,
        end_dt: document.getElementById("analyzeEndDt").value || null,
        clauses: clauses
    };
    if (contractSeq) payload.contract_seq = parseInt(contractSeq);
    if (templateType) payload.template_type = templateType;

    try {
        const response = await fetch(`${API_BASE}/analyze`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "X-Internal-API-Key": getApiKey()
            },
            body: JSON.stringify(payload)
        });

        const data = await response.json();
        resultBox.innerText = JSON.stringify(data, null, 2);
    } catch (e) {
        resultBox.innerText = `Error: ${e.message}\n(서버가 켜져 있는지, 포트가 8000인지 확인하세요)`;
    }
}

// 분석 조항 행 추가
function addAnalyzeClause() {
    const list = document.getElementById("analyzeClauseList");
    const row = document.createElement("div");
    row.className = "analyze-clause-row";
    row.innerHTML = `
        <select class="clause-id-select">
            <option value="INTEREST">INTEREST</option>
            <option value="REPAYMENT_DATE">REPAYMENT_DATE</option>
            <option value="PENALTY">PENALTY</option>
            <option value="COLLATERAL">COLLATERAL</option>
            <option value="GUARANTEE">GUARANTEE</option>
            <option value="OTHER">OTHER</option>
        </select>
        <input type="text" class="clause-title-input" value="" placeholder="조항 제목">
        <input type="text" class="clause-content-input" value="" placeholder="조항 내용">
        <button class="btn-remove" onclick="removeAnalyzeClause(this)" title="삭제">✕</button>
    `;
    list.appendChild(row);
    row.querySelector(".clause-content-input").focus();
}

// 분석 조항 행 삭제
function removeAnalyzeClause(btn) {
    const row = btn.closest(".analyze-clause-row");
    row.remove();
}

// 2. 특약사항 생성
async function testClause() {
    const intent = document.getElementById("clauseIntent").value;
    const resultBox = document.getElementById("clauseResult");

    resultBox.innerText = "법률 문장 생성 중...";
    resultBox.className = "result-box";

    try {
        const response = await fetch(`${API_BASE}/clauses/generate`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "X-Internal-API-Key": getApiKey()
            },
            body: JSON.stringify({
                user_intent: intent,
                contract_context: "" // 테스트용 빈 컨텍스트
            })
        });

        const data = await response.json();
        // 거부 응답 시 경고 스타일 적용
        if (data.rejected) {
            resultBox.className = "result-box result-rejected";
            resultBox.innerText = `⛔ 요청이 거부되었습니다.\n\n사유: ${data.rejection_reason}`;
        } else {
            resultBox.className = "result-box";
            resultBox.innerText = JSON.stringify(data, null, 2);
        }
    } catch (e) {
        resultBox.innerText = `Error: ${e.message}`;
    }
}

// 3. 챗봇
async function testChat() {
    const message = document.getElementById("chatMessage").value;
    const resultBox = document.getElementById("chatResult");

    resultBox.innerText = "AI 챗봇 타이핑 중...";

    try {
        const response = await fetch(`${API_BASE}/chat`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "X-Internal-API-Key": getApiKey()
            },
            body: JSON.stringify({
                message: message,
                history: [] // 테스트용 빈 히스토리
            })
        });

        const data = await response.json();
        resultBox.innerText = JSON.stringify(data, null, 2);
    } catch (e) {
        resultBox.innerText = `Error: ${e.message}`;
    }
}

// ─── 4. 채팅 내역 기반 특약 생성 ───────────────────────────────

// 채팅 메시지 행 추가
function addChatMsg() {
    const list = document.getElementById("chatMessageList");
    const row = document.createElement("div");
    row.className = "chat-msg-row";
    row.innerHTML = `
        <select class="sender-select">
            <option value="105">사용자A (105)</option>
            <option value="106">사용자B (106)</option>
        </select>
        <input type="text" class="chat-msg-input" value="" placeholder="메시지 입력">
        <button class="btn-remove" onclick="removeChatMsg(this)" title="삭제">✕</button>
    `;
    list.appendChild(row);
    // 새로 추가된 input에 포커스
    row.querySelector(".chat-msg-input").focus();
}

// 채팅 메시지 행 삭제
function removeChatMsg(btn) {
    const row = btn.closest(".chat-msg-row");
    if (document.querySelectorAll(".chat-msg-row").length > 1) {
        row.remove();
    } else {
        alert("최소 1개의 메시지는 필요합니다.");
    }
}

// 채팅 → 특약 생성 API 호출
async function testClauseFromChat() {
    const resultBox = document.getElementById("clauseChatResult");
    const templateType = document.getElementById("clauseChatType").value;
    const context = document.getElementById("clauseChatContext").value;

    // 채팅 메시지 수집
    const rows = document.querySelectorAll("#chatMessageList .chat-msg-row");
    const chatMessages = [];
    rows.forEach(row => {
        const senderSeq = parseInt(row.querySelector(".sender-select").value);
        const content = row.querySelector(".chat-msg-input").value.trim();
        if (content) {
            chatMessages.push({
                sender_seq: senderSeq,
                content: content,
                msg_type: "TEXT"
            });
        }
    });

    if (chatMessages.length === 0) {
        resultBox.innerText = "⚠️ 최소 1개의 채팅 메시지를 입력해주세요.";
        return;
    }

    resultBox.innerText = `📨 ${chatMessages.length}건의 채팅 메시지 분석 중... (AI 응답 대기)`;

    const payload = {
        chat_messages: chatMessages,
        contract_context: context || "",
    };
    if (templateType) payload.template_type = templateType;

    resultBox.className = "result-box";

    try {
        const response = await fetch(`${API_BASE}/clauses/from-chat`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "X-Internal-API-Key": getApiKey()
            },
            body: JSON.stringify(payload)
        });

        const data = await response.json();
        // 거부 응답 시 경고 스타일 적용
        if (data.rejected) {
            resultBox.className = "result-box result-rejected";
            resultBox.innerText = `⛔ 요청이 거부되었습니다.\n\n사유: ${data.rejection_reason}`;
        } else {
            resultBox.className = "result-box";
            resultBox.innerText = JSON.stringify(data, null, 2);
        }
    } catch (e) {
        resultBox.innerText = `Error: ${e.message}\n(서버가 켜져 있는지, 포트가 8000인지 확인하세요)`;
    }
}


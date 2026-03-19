const API_BASE = "/internal/v1";

function getApiKey() {
    return document.getElementById("apiKey").value;
}

// 1. 차용증 독소조항 / 세금 분석
async function testAnalyze() {
    const text = document.getElementById("analyzeText").value;
    const principal = document.getElementById("principal").value;
    const interest = document.getElementById("interest").value;
    const resultBox = document.getElementById("analyzeResult");

    resultBox.innerText = "분석 중... (AI 응답 대기)";

    try {
        const response = await fetch(`${API_BASE}/analyze`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "X-Internal-API-Key": getApiKey()
            },
            body: JSON.stringify({
                contract_text: text,
                mode: "full",
                principal_amount: parseInt(principal) || 0,
                interest_rate: parseFloat(interest) || 0
            })
        });

        const data = await response.json();
        resultBox.innerText = JSON.stringify(data, null, 2);
    } catch (e) {
        resultBox.innerText = `Error: ${e.message}\n(서버가 켜져 있는지, 포트가 8000인지 확인하세요)`;
    }
}

// 2. 특약사항 생성
async function testClause() {
    const intent = document.getElementById("clauseIntent").value;
    const resultBox = document.getElementById("clauseResult");

    resultBox.innerText = "법률 문장 생성 중...";

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
        resultBox.innerText = JSON.stringify(data, null, 2);
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

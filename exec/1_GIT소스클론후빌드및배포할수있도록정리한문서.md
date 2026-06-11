
# 프로젝트 개발 및 배포 가이드라인

본 문서는 GitLab 소스 클론 이후 환경 구성, 빌드 및 배포를 위한 가이드를 제공합니다.

---

## 1. 개발 및 실행 환경 (Tech Stack)

프로젝트 구동을 위해 아래의 기술 스택 및 버전을 준수해야 합니다.

| 구분 | 제품명 | 버전 | 비고 |
| :--- | :--- | :--- | :--- |
| **JVM** | Eclipse Temurin | 21.0.10 | Java 21 |
| **Web Server** | Nginx | 1.24.0 | 리버스 프록시 및 정적 리소스 서빙 |
| **WAS** | Apache Tomcat | 10.1.52 | Spring Boot 3.5.11 내장 |
| **IDE** | IntelliJ IDEA | 2025.3.1 | 개발 도구 |

---

## 2. 외부 서비스 및 인프라 설정

프로젝트에서 사용하는 외부 데이터베이스 및 API 서비스 정보입니다.

### 데이터베이스 (DB / Cache)
* **MySQL (Azure)**
    * `URL`: `${SPRING_DATASOURCE_URL}`
    * `Username`: `${SPRING_DATASOURCE_USERNAME}`
    * `Password`: `${SPRING_DATASOURCE_PASSWORD}`
* **MongoDB (Atlas)**
    * `URI`: `${SPRING_DATA_MONGODB_URI}`
* **Redis (Cloud)**
    * `Host`: `${SPRING_DATA_REDIS_HOST}`
    * `Port`: `${SPRING_DATA_REDIS_PORT}`
    * `Password`: `${SPRING_DATA_REDIS_PASSWORD}`

### API 및 보안 키
* **JWT Security**
    * `Secret Key`: `${JWT_SECRET}`
* **External APIs**
    * `SSAFY API KEY`: `${SSAFY_API_KEY}`
    * `Kakao Client ID/Secret`: (개인 보안 이슈로 비공개 - 환경 변수 설정 필요)
* **AI Server**
    * `URL`: `${AI_SERVER_URL}`
    * `Internal API Key`: `${AI_INTERNAL_API_KEY}`

---

## 3. 주요 설정 파일 및 프로퍼티 관리

빌드 전 아래 파일들에 주요 계정 정보가 올바르게 정의되어 있는지 확인하십시오.

* `src/main/resources/application.yml` (또는 `application.properties`)
* `src/main/resources/application-db.yml` (DB 관련 설정 분리 시)
* **환경 변수 관리**: 배포 환경(EC2)에서는 `.env` 파일 또는 시스템 환경 변수를 통해 민감 정보를 주입하는 것을 권장합니다.

---

## 4. 빌드 및 배포 (Deployment)


### EC2 배포 및 방화벽 설정
배포 환경에서는 `ufw`를 통해 아래 포트들이 개방되어 있어야 정상적인 서비스가 가능합니다.

| Port | Protocol | Purpose |
| :--- | :--- | :--- |
| **22** | SSH | 원격 접속 및 관리 |
| **80** | HTTP | 웹 서비스 접속 (Nginx) |
| **443** | HTTPS | 보안 웹 서비스 접속 |
| **8000** | TCP | AI 서버 통신 |
| **8989** | TCP | 서비스 관련 커스텀 포트 |
| **9090** | TCP | 모니터링 또는 추가 서비스 포트 |

**UFW 설정 명령어:**
```bash
sudo ufw allow 22,80,443,8000,8989,9090/tcp
sudo ufw enable
```

---

## 5. 배포 시 특이사항
* **Nginx 설정**: Nginx 1.24.0을 사용하여 80/443 포트로 들어오는 요청을 내장 톰캣(Tomcat 10.1.52)으로 프록시 패스 설정이 필요합니다.
* **Java 버전 확인**: 서버 환경에 `Java 21`이 설치되어 있는지 반드시 확인하십시오 (`java -version`).
* **보안**: Kakao API 키와 같이 비공개된 정보는 CI/CD 도구(GitLab Runner)의 Variables 기능을 통해 주입하십시오.

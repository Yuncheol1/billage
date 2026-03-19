-- Spring Boot 실행 시 TermsMasterDataSeeder가 동일 데이터를 멱등 삽입하도록 함. 수동 실행 시에도 아래 NOT EXISTS로 중복 방지.

INSERT INTO terms_master (terms_key, terms_type, terms_content, terms_version, created_dttm)
SELECT 'service_terms', 'mandatory', '빌리지 서비스 이용을 위해 서비스 이용약관에 동의합니다. 본 약관에는 서비스 이용 조건, 회원의 권리와 의무, 운영 정책이 포함됩니다.', 'v1.0', NOW(6)
WHERE NOT EXISTS (
    SELECT 1
    FROM terms_master
    WHERE terms_key = 'service_terms' AND terms_version = 'v1.0'
);

INSERT INTO terms_master (terms_key, terms_type, terms_content, terms_version, created_dttm)
SELECT 'privacy_policy', 'mandatory', '회원가입 및 서비스 제공을 위해 이메일, 휴대폰 번호 등 필요한 개인정보를 수집하고 이용하는 것에 동의합니다.', 'v1.0', NOW(6)
WHERE NOT EXISTS (
    SELECT 1
    FROM terms_master
    WHERE terms_key = 'privacy_policy' AND terms_version = 'v1.0'
);

INSERT INTO terms_master (terms_key, terms_type, terms_content, terms_version, created_dttm)
SELECT 'marketing_notifications', 'optional', '이벤트, 혜택, 프로모션 등 마케팅 정보 수신에 동의합니다. 동의하지 않아도 기본 서비스 이용은 가능합니다.', 'v1.0', NOW(6)
WHERE NOT EXISTS (
    SELECT 1
    FROM terms_master
    WHERE terms_key = 'marketing_notifications' AND terms_version = 'v1.0'
);

INSERT INTO terms_master (terms_key, terms_type, terms_content, terms_version, created_dttm)
SELECT 'personalized_recommendations', 'optional', '서비스 이용 기록을 바탕으로 맞춤형 추천 및 개인화된 안내를 제공받는 것에 동의합니다.', 'v1.0', NOW(6)
WHERE NOT EXISTS (
    SELECT 1
    FROM terms_master
    WHERE terms_key = 'personalized_recommendations' AND terms_version = 'v1.0'
);

-- 실행 방법
--   mysql -u root < db/init.sql
-- 또는 mysql -u root 로 접속한 뒤 이 파일 내용을 붙여넣는다.

-- utf8mb4 필수: 매장명·리뷰에 이모지가 들어오면 utf8 로는 저장이 안 된다.
CREATE DATABASE IF NOT EXISTS petcheck
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'petcheck'@'localhost' IDENTIFIED BY '원하는비밀번호';
GRANT ALL PRIVILEGES ON petcheck.* TO 'petcheck'@'localhost';
FLUSH PRIVILEGES;

-- 테이블은 JPA(ddl-auto: update)가 엔티티를 보고 자동 생성한다.
-- 여기서 CREATE TABLE 을 직접 쓰지 않는다.

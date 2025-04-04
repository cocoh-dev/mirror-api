-- V4__change_role_to_varchar_and_uppercase.sql

-- 1. ENUM → VARCHAR 변경
ALTER TABLE users
MODIFY COLUMN role VARCHAR(20) NOT NULL;

-- 2. 기존 소문자 값을 대문자로 업데이트
UPDATE users
SET role = UPPER(role);

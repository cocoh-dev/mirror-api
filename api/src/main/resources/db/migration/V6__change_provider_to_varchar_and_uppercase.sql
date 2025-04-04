-- ENUM -> VARCHAR(20) 으로 변경
ALTER TABLE users
MODIFY COLUMN provider VARCHAR(20) NOT NULL;

-- 이후 UPPER() 적용
UPDATE users
SET provider = UPPER(provider);

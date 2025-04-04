-- 기존 provider 값을 대문자로 변환
UPDATE users SET provider = 'LOCAL' WHERE provider = 'local';
UPDATE users SET provider = 'GOOGLE' WHERE provider = 'google';
UPDATE users SET provider = 'KAKAO' WHERE provider = 'kakao';
UPDATE users SET provider = 'NAVER' WHERE provider = 'naver';

-- 인덱스나 제약조건이 필요하다면 여기에 추가
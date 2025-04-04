UPDATE users
SET provider = UPPER(provider)
WHERE provider IS NOT NULL;

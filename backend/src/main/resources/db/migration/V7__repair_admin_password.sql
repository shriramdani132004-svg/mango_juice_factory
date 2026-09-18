-- V7: Repair corrupted admin password hash
-- Previous hash was corrupted by PowerShell escaping of BCrypt '$' characters
-- This restores the valid BCrypt hash for password 'admin123'

UPDATE users
SET password_hash = '$2a$10$WhBGYdFrB1jsf9hErmqAkuDGANy8b0zsRRmzA7VFH3YiOEMMkmsca',
    updated_at = NOW()
WHERE username = 'admin'
  AND (password_hash IS NULL
       OR length(password_hash) != 60
       OR password_hash !~ '^\$2[aby]\$');

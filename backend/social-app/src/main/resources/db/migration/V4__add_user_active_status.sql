-- Add active status column to users table
ALTER TABLE users 
ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

-- Ensure the admin account is always active
UPDATE users 
SET active = TRUE 
WHERE username = 'admin';
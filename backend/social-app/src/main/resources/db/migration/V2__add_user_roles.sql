-- Add role column to users table
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Create the first admin account
-- Username: admin
-- Password: admin123
-- The password hash below is BCrypt hash of "admin123"
INSERT INTO users (username, email, password_hash, role, created_at)
VALUES ('admin', 'admin@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ADMIN', NOW())
ON DUPLICATE KEY UPDATE role = 'ADMIN';
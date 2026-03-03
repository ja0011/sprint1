-- Add profile fields to users table
ALTER TABLE users 
ADD COLUMN bio VARCHAR(500),
ADD COLUMN graduation_year INT,
ADD COLUMN major VARCHAR(100),
ADD COLUMN minor VARCHAR(100),
ADD COLUMN profile_picture_url VARCHAR(255);

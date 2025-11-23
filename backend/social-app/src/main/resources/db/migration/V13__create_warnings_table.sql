CREATE TABLE IF NOT EXISTS warnings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT warnings_ibfk_1 FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT warnings_ibfk_2 FOREIGN KEY (admin_id) REFERENCES users (id) ON DELETE CASCADE,
    KEY idx_warnings_user_id (user_id),
    KEY idx_warnings_admin_id (admin_id),
    KEY idx_warnings_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
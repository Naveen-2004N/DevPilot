
-- DevPilot MySQL Initialization

-- Create documents table
CREATE TABLE IF NOT EXISTS documents (
    id CHAR(36) PRIMARY KEY,
    content LONGTEXT NOT NULL,
    embedding JSON,
    metadata JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE failed_sync_events (
                                    id VARCHAR(36) PRIMARY KEY,
                                    exchange_name VARCHAR(100),
                                    routing_key VARCHAR(100),
                                    payload TEXT NOT NULL,
                                    error_reason TEXT,
                                    status VARCHAR(50) DEFAULT 'FA  ILED', -- Trạng thái: FAILED, RESOLVED
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
-- Push tokens for mobile push notifications
CREATE TABLE notification.push_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(500) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    active BOOLEAN DEFAULT TRUE,
    CONSTRAINT uk_push_token_token UNIQUE (token)
);

CREATE INDEX idx_push_tokens_user_id ON notification.push_tokens(user_id);
CREATE INDEX idx_push_tokens_active ON notification.push_tokens(active);

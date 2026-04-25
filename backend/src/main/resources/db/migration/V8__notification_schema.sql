-- V8: notification schema - notifications and push_tokens
CREATE TABLE notification.notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    user_type VARCHAR(20) NOT NULL,
    type VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    data JSONB,
    read BOOLEAN DEFAULT false NOT NULL,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    read_at TIMESTAMP
);

CREATE TABLE notification.push_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL,
    platform VARCHAR(255) NOT NULL,
    active BOOLEAN DEFAULT true NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Indexes
CREATE INDEX idx_notifications_user_id ON notification.notifications(user_id);
CREATE INDEX idx_notifications_user_type ON notification.notifications(user_type);
CREATE INDEX idx_notifications_user_sent ON notification.notifications(user_id, sent_at);
CREATE INDEX idx_notifications_type ON notification.notifications(type);
CREATE INDEX idx_notifications_read ON notification.notifications(read);
CREATE INDEX idx_notifications_sent_at ON notification.notifications(sent_at);
CREATE INDEX idx_push_tokens_user_id ON notification.push_tokens(user_id);
CREATE INDEX idx_push_tokens_active ON notification.push_tokens(active);

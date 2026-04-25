-- V11: seed user data - admin user for testing
-- Password: Admin123
INSERT INTO auth.users (email, password_hash, role, mfa_enabled, account_locked, account_expired, credentials_expired, enabled, created_at, updated_at) VALUES
('admin@test.com', '$2a$10$Jj.mi1ax2YHgeQ/yjWCYdudBliP2QAkSDPWrARdYcJboY/N3Felli', 'SUPERADMIN', false, false, false, false, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

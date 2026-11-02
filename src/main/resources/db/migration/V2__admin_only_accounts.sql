-- Disable legacy limited-user accounts so they do not gain CEO/Admin access.
UPDATE users SET role = 'ADMIN', active = FALSE WHERE role <> 'ADMIN';

-- V1 created this unnamed MySQL CHECK as users_chk_1.
ALTER TABLE users DROP CHECK users_chk_1;
ALTER TABLE users MODIFY role VARCHAR(10) NOT NULL DEFAULT 'ADMIN';
ALTER TABLE users ADD CONSTRAINT chk_users_admin_only CHECK (role = 'ADMIN');
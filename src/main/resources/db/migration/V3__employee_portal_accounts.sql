ALTER TABLE users ADD COLUMN employee_id VARCHAR(30) NULL;
CREATE UNIQUE INDEX uq_users_employee_id ON users(employee_id);
ALTER TABLE users DROP CHECK chk_users_admin_only;
ALTER TABLE users ADD CONSTRAINT chk_users_role_link
 CHECK ((role = 'ADMIN' AND employee_id IS NULL) OR (role = 'EMPLOYEE' AND employee_id IS NOT NULL));
ALTER TABLE users ADD CONSTRAINT fk_users_employee
 FOREIGN KEY (employee_id) REFERENCES employees(employee_id) ON UPDATE CASCADE ON DELETE RESTRICT;
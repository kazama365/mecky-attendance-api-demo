CREATE TABLE users (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, username VARCHAR(60) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL, role VARCHAR(10) NOT NULL CHECK(role IN ('ADMIN','USER')),
 active BOOLEAN NOT NULL DEFAULT TRUE, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
CREATE TABLE employees (
 employee_id VARCHAR(30) PRIMARY KEY, full_name VARCHAR(120) NOT NULL, position VARCHAR(80),
 contact_number VARCHAR(30), address VARCHAR(300), active BOOLEAN NOT NULL DEFAULT TRUE,
 sensor_slot_id INT UNIQUE, daily_rate DECIMAL(10,2) NOT NULL DEFAULT 0.00,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, INDEX idx_employee_name(full_name), INDEX idx_employee_active(active)
) ENGINE=InnoDB;
CREATE TABLE schedules (
 employee_id VARCHAR(30) PRIMARY KEY, working_days VARCHAR(40) NOT NULL,
 expected_time_in TIME NOT NULL, expected_time_out TIME NOT NULL, grace_minutes INT NOT NULL DEFAULT 5,
 min_scan_gap_seconds INT NOT NULL DEFAULT 60,
 CONSTRAINT fk_schedule_employee FOREIGN KEY(employee_id) REFERENCES employees(employee_id) ON UPDATE CASCADE ON DELETE CASCADE,
 CHECK(grace_minutes>=0), CHECK(min_scan_gap_seconds>=0)
) ENGINE=InnoDB;
CREATE TABLE absences (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, employee_id VARCHAR(30) NOT NULL, absence_date DATE NOT NULL,
 reason VARCHAR(20) NOT NULL CHECK(reason IN ('Sick','Emergency','Leave','Unexcused')),
 notes VARCHAR(500), recorded_by VARCHAR(60) NOT NULL,
 UNIQUE KEY uq_absence_day(employee_id,absence_date), INDEX idx_absence_date(absence_date),
 CONSTRAINT fk_absence_employee FOREIGN KEY(employee_id) REFERENCES employees(employee_id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;
CREATE TABLE attendance (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, employee_id VARCHAR(30) NOT NULL, attendance_date DATE NOT NULL,
 time_in DATETIME, time_out DATETIME, status VARCHAR(20) NOT NULL,
 late_minutes INT NOT NULL DEFAULT 0, hours_worked DECIMAL(6,2) NOT NULL DEFAULT 0.00,
 scan_uuid VARCHAR(36) UNIQUE, UNIQUE KEY uq_attendance_day(employee_id,attendance_date),
 INDEX idx_attendance_date(attendance_date), INDEX idx_attendance_employee_date(employee_id,attendance_date),
 CONSTRAINT fk_attendance_employee FOREIGN KEY(employee_id) REFERENCES employees(employee_id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;
CREATE TABLE scan_events (
 scan_uuid VARCHAR(36) PRIMARY KEY, sensor_slot_id INT NOT NULL, scanned_at DATETIME NOT NULL,
 outcome VARCHAR(20) NOT NULL, employee_id VARCHAR(30), INDEX idx_scan_time(scanned_at),
 CONSTRAINT fk_scan_employee FOREIGN KEY(employee_id) REFERENCES employees(employee_id) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB;
CREATE TABLE device_commands (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, type VARCHAR(20) NOT NULL, employee_id VARCHAR(30) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PENDING', created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 result_message VARCHAR(300), INDEX idx_command_status(status,created_at),
 CONSTRAINT fk_command_employee FOREIGN KEY(employee_id) REFERENCES employees(employee_id) ON UPDATE CASCADE ON DELETE RESTRICT
) ENGINE=InnoDB;
CREATE TABLE devices (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(80) NOT NULL UNIQUE, api_key_hash CHAR(64) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE', last_seen DATETIME, INDEX idx_device_status(status,last_seen)
) ENGINE=InnoDB;
CREATE TABLE payroll_settings (
 employee_id VARCHAR(30) PRIMARY KEY, daily_rate DECIMAL(10,2) NOT NULL DEFAULT 0.00,
 hourly_mode BOOLEAN NOT NULL DEFAULT FALSE,
 CONSTRAINT fk_payroll_employee FOREIGN KEY(employee_id) REFERENCES employees(employee_id) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB;
CREATE TABLE payroll_records (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, employee_id VARCHAR(30) NOT NULL, period_start DATE NOT NULL,
 period_end DATE NOT NULL, days_present INT NOT NULL, days_absent INT NOT NULL, late_minutes INT NOT NULL DEFAULT 0,
 undertime_minutes INT NOT NULL DEFAULT 0, gross_pay DECIMAL(10,2) NOT NULL, deductions DECIMAL(10,2) NOT NULL,
 net_pay DECIMAL(10,2) NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE KEY uq_payroll_period(employee_id,period_start,period_end),
 CONSTRAINT fk_payroll_record_employee FOREIGN KEY(employee_id) REFERENCES employees(employee_id) ON UPDATE CASCADE ON DELETE RESTRICT,
 CHECK(period_end>=period_start)
) ENGINE=InnoDB;
CREATE TABLE unregistered_scans (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, scan_uuid VARCHAR(36) NOT NULL UNIQUE, sensor_slot_id INT NOT NULL,
 scanned_at DATETIME NOT NULL, INDEX idx_unregistered_time(scanned_at)
) ENGINE=InnoDB;
CREATE TABLE audit_logs (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, actor_username VARCHAR(60) NOT NULL, action VARCHAR(60) NOT NULL,
 entity_name VARCHAR(60) NOT NULL, entity_id VARCHAR(80) NOT NULL, before_json JSON, after_json JSON,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, INDEX idx_audit_entity(entity_name,entity_id), INDEX idx_audit_time(created_at)
) ENGINE=InnoDB;

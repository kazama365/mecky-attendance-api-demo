CREATE TABLE deduction_policies (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    effective_date DATE NOT NULL UNIQUE,
    rate_per_minute DECIMAL(10,4) NOT NULL,
    created_at DATETIME NOT NULL,
    created_by VARCHAR(60) NOT NULL
);

ALTER TABLE payroll_records
    ADD COLUMN late_deduction DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN other_deductions DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN policy_snapshot TEXT NULL;

UPDATE payroll_records SET other_deductions = deductions;

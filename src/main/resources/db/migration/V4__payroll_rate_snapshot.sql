ALTER TABLE payroll_records
    ADD COLUMN daily_rate_snapshot DECIMAL(10,2) NOT NULL DEFAULT 0.00 AFTER days_present;

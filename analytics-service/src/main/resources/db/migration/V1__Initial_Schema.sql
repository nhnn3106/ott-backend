-- Analytics Service Database Schema - Initial Version
-- Created: 2026-03-28
-- Description: Core analytics tables with event sourcing pattern

-- =====================================================
-- 1. SHEDLOCK TABLE (Distributed Job Locking)
-- =====================================================
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) PRIMARY KEY,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL
);

COMMENT ON TABLE shedlock IS 'Distributed lock table for scheduled jobs coordination';
COMMENT ON COLUMN shedlock.name IS 'Unique job name identifier';
COMMENT ON COLUMN shedlock.lock_until IS 'Lock expiration timestamp';
COMMENT ON COLUMN shedlock.locked_at IS 'When lock was acquired';
COMMENT ON COLUMN shedlock.locked_by IS 'Instance/hostname that acquired lock';

-- =====================================================
-- 2. RAW EVENTS LOG (Event Store - Event Sourcing)
-- =====================================================
CREATE TABLE raw_events_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id VARCHAR(100) UNIQUE NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    user_id UUID,
    payload JSONB NOT NULL,
    processed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);

CREATE INDEX idx_raw_events_type ON raw_events_log(event_type);
CREATE INDEX idx_raw_events_processed ON raw_events_log(processed, created_at);
CREATE INDEX idx_raw_events_event_id ON raw_events_log(event_id);
CREATE INDEX idx_raw_events_user_id ON raw_events_log(user_id);
CREATE INDEX idx_raw_events_created_at ON raw_events_log(created_at DESC);

COMMENT ON TABLE raw_events_log IS 'Event store for all RabbitMQ events - single source of truth for analytics';
COMMENT ON COLUMN raw_events_log.event_id IS 'Unique event identifier for idempotency';
COMMENT ON COLUMN raw_events_log.event_type IS 'Event type: USER_REGISTERED, USER_LOGIN, SESSION_CREATED';
COMMENT ON COLUMN raw_events_log.payload IS 'Full event data in JSON format';
COMMENT ON COLUMN raw_events_log.processed IS 'Whether event has been aggregated into metrics';

-- =====================================================
-- 3. DAILY USER METRICS (Aggregated Daily Statistics)
-- =====================================================
CREATE TABLE daily_user_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date DATE NOT NULL UNIQUE,
    new_registrations INT NOT NULL DEFAULT 0,
    daily_active_users INT NOT NULL DEFAULT 0,
    total_users INT NOT NULL DEFAULT 0,
    verified_users INT NOT NULL DEFAULT 0,
    google_users INT NOT NULL DEFAULT 0,
    local_users INT NOT NULL DEFAULT 0,
    two_fa_enabled_users INT NOT NULL DEFAULT 0,
    blocked_users INT NOT NULL DEFAULT 0,
    deleted_users INT NOT NULL DEFAULT 0,
    total_logins INT NOT NULL DEFAULT 0,
    successful_logins INT NOT NULL DEFAULT 0,
    failed_logins INT NOT NULL DEFAULT 0,
    unique_login_users INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_daily_metrics_date ON daily_user_metrics(date DESC);

COMMENT ON TABLE daily_user_metrics IS 'Daily aggregated user metrics';
COMMENT ON COLUMN daily_user_metrics.daily_active_users IS 'DAU - Users who logged in today';
COMMENT ON COLUMN daily_user_metrics.unique_login_users IS 'Distinct users with login attempts';

-- =====================================================
-- 4. MONTHLY USER METRICS (Aggregated Monthly Statistics)
-- =====================================================
CREATE TABLE monthly_user_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    year INT NOT NULL,
    month INT NOT NULL,
    monthly_active_users INT NOT NULL DEFAULT 0,
    new_registrations INT NOT NULL DEFAULT 0,
    churned_users INT NOT NULL DEFAULT 0,
    retention_rate DECIMAL(5,2),
    avg_sessions_per_user DECIMAL(10,2),
    avg_session_duration_minutes DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(year, month)
);

CREATE INDEX idx_monthly_metrics_year_month ON monthly_user_metrics(year DESC, month DESC);

COMMENT ON TABLE monthly_user_metrics IS 'Monthly aggregated user metrics';
COMMENT ON COLUMN monthly_user_metrics.monthly_active_users IS 'MAU - Users who logged in this month';
COMMENT ON COLUMN monthly_user_metrics.churned_users IS 'Users who did not return this month';

-- =====================================================
-- 5. RETENTION COHORT (Cohort Analysis)
-- =====================================================
CREATE TABLE retention_cohort (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cohort_date DATE NOT NULL UNIQUE,
    cohort_size INT NOT NULL,
    day_0_users INT NOT NULL,
    day_1_users INT,
    day_7_users INT,
    day_30_users INT,
    day_60_users INT,
    day_90_users INT,
    retention_1_day DECIMAL(5,2),
    retention_7_day DECIMAL(5,2),
    retention_30_day DECIMAL(5,2),
    retention_60_day DECIMAL(5,2),
    retention_90_day DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_retention_cohort_date ON retention_cohort(cohort_date DESC);

COMMENT ON TABLE retention_cohort IS 'User retention tracking by registration cohort';
COMMENT ON COLUMN retention_cohort.cohort_date IS 'Date when users registered';
COMMENT ON COLUMN retention_cohort.cohort_size IS 'Number of users who registered on cohort_date';
COMMENT ON COLUMN retention_cohort.retention_1_day IS 'Percentage of users who returned after 1 day';

-- =====================================================
-- 6. DEVICE ANALYTICS (Device Type Distribution)
-- =====================================================
CREATE TABLE device_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date DATE NOT NULL,
    device_type VARCHAR(50) NOT NULL,
    total_sessions INT NOT NULL DEFAULT 0,
    unique_users INT NOT NULL DEFAULT 0,
    total_logins INT NOT NULL DEFAULT 0,
    avg_session_duration_minutes DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(date, device_type)
);

CREATE INDEX idx_device_analytics_date ON device_analytics(date DESC, device_type);

COMMENT ON TABLE device_analytics IS 'Daily device type statistics';
COMMENT ON COLUMN device_analytics.device_type IS 'MOBILE, DESKTOP, TABLET, TV, UNKNOWN';

-- =====================================================
-- 7. GEOGRAPHIC ANALYTICS (Geographic Distribution)
-- =====================================================
CREATE TABLE geographic_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date DATE NOT NULL,
    country VARCHAR(100),
    city VARCHAR(200),
    total_sessions INT NOT NULL DEFAULT 0,
    unique_users INT NOT NULL DEFAULT 0,
    total_logins INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(date, country, city)
);

CREATE INDEX idx_geo_analytics_date ON geographic_analytics(date DESC);
CREATE INDEX idx_geo_analytics_country ON geographic_analytics(country);

COMMENT ON TABLE geographic_analytics IS 'Daily geographic distribution statistics';

-- =====================================================
-- 8. LOGIN METHOD ANALYTICS (Login Method Distribution)
-- =====================================================
CREATE TABLE login_method_analytics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date DATE NOT NULL,
    login_method VARCHAR(50) NOT NULL,
    total_attempts INT NOT NULL DEFAULT 0,
    successful_attempts INT NOT NULL DEFAULT 0,
    failed_attempts INT NOT NULL DEFAULT 0,
    unique_users INT NOT NULL DEFAULT 0,
    success_rate DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(date, login_method)
);

CREATE INDEX idx_login_method_date ON login_method_analytics(date DESC);

COMMENT ON TABLE login_method_analytics IS 'Daily login method statistics';
COMMENT ON COLUMN login_method_analytics.login_method IS 'LOCAL, GOOGLE, OTP, QR_CODE';
COMMENT ON COLUMN login_method_analytics.success_rate IS 'Percentage of successful logins';

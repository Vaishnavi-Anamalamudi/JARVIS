CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(80) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT roles_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT roles_deleted_after_created CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX ux_roles_name_active ON roles (lower(name)) WHERE deleted_at IS NULL;

CREATE TABLE app_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES roles (id),
    username VARCHAR(80) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(160) NOT NULL,
    status VARCHAR(40) NOT NULL,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT app_users_username_not_blank CHECK (length(trim(username)) >= 3),
    CONSTRAINT app_users_email_not_blank CHECK (length(trim(email)) > 0),
    CONSTRAINT app_users_password_hash_not_blank CHECK (length(trim(password_hash)) > 0),
    CONSTRAINT app_users_full_name_not_blank CHECK (length(trim(full_name)) > 0),
    CONSTRAINT app_users_status_valid CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DISABLED', 'PENDING_VERIFICATION')),
    CONSTRAINT app_users_deleted_after_created CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX ux_app_users_username_active ON app_users (lower(username)) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX ux_app_users_email_active ON app_users (lower(email)) WHERE deleted_at IS NULL;
CREATE INDEX ix_app_users_role_id ON app_users (role_id);
CREATE INDEX ix_app_users_status ON app_users (status);

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT refresh_tokens_token_hash_not_blank CHECK (length(trim(token_hash)) > 0),
    CONSTRAINT refresh_tokens_expires_after_created CHECK (expires_at > created_at),
    CONSTRAINT refresh_tokens_revoked_after_created CHECK (revoked_at IS NULL OR revoked_at >= created_at),
    CONSTRAINT refresh_tokens_deleted_after_created CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX ux_refresh_tokens_token_hash_active ON refresh_tokens (token_hash) WHERE deleted_at IS NULL;
CREATE INDEX ix_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX ix_refresh_tokens_expires_at ON refresh_tokens (expires_at);

CREATE TABLE login_audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES app_users (id),
    username_attempted VARCHAR(80) NOT NULL,
    outcome VARCHAR(40) NOT NULL,
    ip_address INET,
    user_agent TEXT,
    failure_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT login_audit_logs_username_not_blank CHECK (length(trim(username_attempted)) > 0),
    CONSTRAINT login_audit_logs_outcome_valid CHECK (outcome IN ('SUCCESS', 'FAILED', 'LOCKED', 'TOKEN_REFRESHED', 'LOGOUT')),
    CONSTRAINT login_audit_logs_failure_reason_required CHECK (
        (outcome = 'FAILED' AND failure_reason IS NOT NULL AND length(trim(failure_reason)) > 0)
        OR outcome <> 'FAILED'
    ),
    CONSTRAINT login_audit_logs_deleted_after_created CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE INDEX ix_login_audit_logs_user_id ON login_audit_logs (user_id);
CREATE INDEX ix_login_audit_logs_created_at ON login_audit_logs (created_at DESC);
CREATE INDEX ix_login_audit_logs_outcome ON login_audit_logs (outcome);

CREATE TABLE api_consumers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL REFERENCES app_users (id),
    name VARCHAR(140) NOT NULL,
    description VARCHAR(1000),
    contact_email VARCHAR(254) NOT NULL,
    status VARCHAR(40) NOT NULL,
    environment VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT api_consumers_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT api_consumers_contact_email_not_blank CHECK (length(trim(contact_email)) > 0),
    CONSTRAINT api_consumers_status_valid CHECK (status IN ('ACTIVE', 'SUSPENDED', 'DISABLED', 'PENDING_APPROVAL')),
    CONSTRAINT api_consumers_environment_valid CHECK (environment IN ('DEVELOPMENT', 'STAGING', 'PRODUCTION')),
    CONSTRAINT api_consumers_deleted_after_created CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX ux_api_consumers_owner_name_env_active
    ON api_consumers (owner_user_id, lower(name), environment)
    WHERE deleted_at IS NULL;
CREATE INDEX ix_api_consumers_owner_user_id ON api_consumers (owner_user_id);
CREATE INDEX ix_api_consumers_status ON api_consumers (status);

CREATE TABLE api_consumer_credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consumer_id UUID NOT NULL REFERENCES api_consumers (id) ON DELETE CASCADE,
    key_prefix VARCHAR(32) NOT NULL,
    credential_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT api_consumer_credentials_key_prefix_not_blank CHECK (length(trim(key_prefix)) >= 6),
    CONSTRAINT api_consumer_credentials_hash_not_blank CHECK (length(trim(credential_hash)) > 0),
    CONSTRAINT api_consumer_credentials_expires_after_created CHECK (expires_at IS NULL OR expires_at > created_at),
    CONSTRAINT api_consumer_credentials_revoked_after_created CHECK (revoked_at IS NULL OR revoked_at >= created_at),
    CONSTRAINT api_consumer_credentials_deleted_after_created CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX ux_api_consumer_credentials_key_prefix_active
    ON api_consumer_credentials (key_prefix)
    WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX ux_api_consumer_credentials_hash_active
    ON api_consumer_credentials (credential_hash)
    WHERE deleted_at IS NULL;
CREATE INDEX ix_api_consumer_credentials_consumer_id ON api_consumer_credentials (consumer_id);

CREATE TABLE upstream_services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(140) NOT NULL,
    base_url VARCHAR(2048) NOT NULL,
    health_check_path VARCHAR(512),
    status VARCHAR(40) NOT NULL,
    timeout_ms INTEGER NOT NULL,
    retry_count INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT upstream_services_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT upstream_services_base_url_http CHECK (base_url ~ '^https?://'),
    CONSTRAINT upstream_services_status_valid CHECK (status IN ('ACTIVE', 'DISABLED', 'DEGRADED')),
    CONSTRAINT upstream_services_timeout_positive CHECK (timeout_ms BETWEEN 100 AND 120000),
    CONSTRAINT upstream_services_retry_count_valid CHECK (retry_count BETWEEN 0 AND 10),
    CONSTRAINT upstream_services_deleted_after_created CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX ux_upstream_services_name_active ON upstream_services (lower(name)) WHERE deleted_at IS NULL;
CREATE INDEX ix_upstream_services_status ON upstream_services (status);

CREATE TABLE gateway_routes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    upstream_service_id UUID NOT NULL REFERENCES upstream_services (id),
    route_key VARCHAR(120) NOT NULL,
    name VARCHAR(160) NOT NULL,
    path_pattern VARCHAR(512) NOT NULL,
    allowed_methods TEXT[] NOT NULL,
    strip_prefix INTEGER NOT NULL,
    priority INTEGER NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT gateway_routes_route_key_not_blank CHECK (length(trim(route_key)) > 0),
    CONSTRAINT gateway_routes_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT gateway_routes_path_pattern_valid CHECK (path_pattern LIKE '/%'),
    CONSTRAINT gateway_routes_allowed_methods_present CHECK (array_length(allowed_methods, 1) > 0),
    CONSTRAINT gateway_routes_strip_prefix_valid CHECK (strip_prefix >= 0),
    CONSTRAINT gateway_routes_priority_valid CHECK (priority >= 0),
    CONSTRAINT gateway_routes_status_valid CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT gateway_routes_deleted_after_created CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX ux_gateway_routes_route_key_active ON gateway_routes (lower(route_key)) WHERE deleted_at IS NULL;
CREATE INDEX ix_gateway_routes_upstream_service_id ON gateway_routes (upstream_service_id);
CREATE INDEX ix_gateway_routes_status_priority ON gateway_routes (status, priority);

CREATE TABLE gateway_route_predicates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    route_id UUID NOT NULL REFERENCES gateway_routes (id) ON DELETE CASCADE,
    predicate_type VARCHAR(60) NOT NULL,
    predicate_config JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT gateway_route_predicates_type_valid CHECK (predicate_type IN ('PATH', 'METHOD', 'HEADER', 'QUERY', 'HOST')),
    CONSTRAINT gateway_route_predicates_config_object CHECK (jsonb_typeof(predicate_config) = 'object'),
    CONSTRAINT gateway_route_predicates_deleted_after_created CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE INDEX ix_gateway_route_predicates_route_id ON gateway_route_predicates (route_id);
CREATE INDEX ix_gateway_route_predicates_type ON gateway_route_predicates (predicate_type);

CREATE TABLE rate_limit_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(160) NOT NULL,
    algorithm VARCHAR(40) NOT NULL,
    window_seconds INTEGER NOT NULL,
    max_requests INTEGER NOT NULL,
    bucket_capacity INTEGER,
    refill_tokens INTEGER,
    refill_period_seconds INTEGER,
    strictness_factor NUMERIC(8,4) NOT NULL,
    adaptive_enabled BOOLEAN NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT rate_limit_policies_name_not_blank CHECK (length(trim(name)) > 0),
    CONSTRAINT rate_limit_policies_algorithm_valid CHECK (algorithm IN ('SLIDING_WINDOW', 'TOKEN_BUCKET', 'HYBRID')),
    CONSTRAINT rate_limit_policies_window_positive CHECK (window_seconds > 0),
    CONSTRAINT rate_limit_policies_max_requests_positive CHECK (max_requests > 0),
    CONSTRAINT rate_limit_policies_bucket_valid CHECK (
        (algorithm = 'SLIDING_WINDOW' AND bucket_capacity IS NULL AND refill_tokens IS NULL AND refill_period_seconds IS NULL)
        OR (algorithm IN ('TOKEN_BUCKET', 'HYBRID') AND bucket_capacity > 0 AND refill_tokens > 0 AND refill_period_seconds > 0)
    ),
    CONSTRAINT rate_limit_policies_strictness_valid CHECK (strictness_factor >= 0.1000 AND strictness_factor <= 10.0000),
    CONSTRAINT rate_limit_policies_status_valid CHECK (status IN ('ACTIVE', 'DISABLED', 'ARCHIVED')),
    CONSTRAINT rate_limit_policies_deleted_after_created CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX ux_rate_limit_policies_name_active ON rate_limit_policies (lower(name)) WHERE deleted_at IS NULL;
CREATE INDEX ix_rate_limit_policies_status ON rate_limit_policies (status);

CREATE TABLE rate_limit_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id UUID NOT NULL REFERENCES rate_limit_policies (id),
    consumer_id UUID REFERENCES api_consumers (id),
    route_id UUID REFERENCES gateway_routes (id),
    priority INTEGER NOT NULL,
    status VARCHAR(40) NOT NULL,
    valid_from TIMESTAMPTZ NOT NULL,
    valid_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT rate_limit_assignments_scope_present CHECK (consumer_id IS NOT NULL OR route_id IS NOT NULL),
    CONSTRAINT rate_limit_assignments_priority_valid CHECK (priority >= 0),
    CONSTRAINT rate_limit_assignments_status_valid CHECK (status IN ('ACTIVE', 'DISABLED', 'EXPIRED')),
    CONSTRAINT rate_limit_assignments_valid_window CHECK (valid_until IS NULL OR valid_until > valid_from),
    CONSTRAINT rate_limit_assignments_deleted_after_created CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE INDEX ix_rate_limit_assignments_policy_id ON rate_limit_assignments (policy_id);
CREATE INDEX ix_rate_limit_assignments_consumer_route ON rate_limit_assignments (consumer_id, route_id);
CREATE INDEX ix_rate_limit_assignments_status_priority ON rate_limit_assignments (status, priority);

CREATE TABLE request_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    correlation_id UUID NOT NULL,
    consumer_id UUID REFERENCES api_consumers (id),
    route_id UUID REFERENCES gateway_routes (id),
    upstream_service_id UUID REFERENCES upstream_services (id),
    request_method VARCHAR(12) NOT NULL,
    request_path VARCHAR(2048) NOT NULL,
    request_query_hash VARCHAR(128),
    request_headers_hash VARCHAR(128),
    source_ip INET,
    user_agent TEXT,
    gateway_outcome VARCHAR(40) NOT NULL,
    status_code INTEGER,
    response_time_ms INTEGER,
    request_bytes BIGINT NOT NULL,
    response_bytes BIGINT NOT NULL,
    error_code VARCHAR(120),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT request_logs_method_not_blank CHECK (length(trim(request_method)) > 0),
    CONSTRAINT request_logs_path_valid CHECK (request_path LIKE '/%'),
    CONSTRAINT request_logs_outcome_valid CHECK (gateway_outcome IN ('ALLOWED', 'BLOCKED', 'ERROR')),
    CONSTRAINT request_logs_status_code_valid CHECK (status_code IS NULL OR status_code BETWEEN 100 AND 599),
    CONSTRAINT request_logs_response_time_valid CHECK (response_time_ms IS NULL OR response_time_ms >= 0),
    CONSTRAINT request_logs_bytes_valid CHECK (request_bytes >= 0 AND response_bytes >= 0),
    CONSTRAINT request_logs_completed_after_started CHECK (completed_at IS NULL OR completed_at >= started_at),
    CONSTRAINT request_logs_deleted_after_created CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX ux_request_logs_correlation_id_active ON request_logs (correlation_id) WHERE deleted_at IS NULL;
CREATE INDEX ix_request_logs_consumer_started ON request_logs (consumer_id, started_at DESC);
CREATE INDEX ix_request_logs_route_started ON request_logs (route_id, started_at DESC);
CREATE INDEX ix_request_logs_outcome_started ON request_logs (gateway_outcome, started_at DESC);
CREATE INDEX ix_request_logs_status_started ON request_logs (status_code, started_at DESC);

CREATE TABLE rate_limit_decisions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_log_id UUID NOT NULL REFERENCES request_logs (id) ON DELETE CASCADE,
    policy_id UUID NOT NULL REFERENCES rate_limit_policies (id),
    assignment_id UUID REFERENCES rate_limit_assignments (id),
    algorithm VARCHAR(40) NOT NULL,
    decision VARCHAR(40) NOT NULL,
    effective_limit INTEGER NOT NULL,
    observed_count INTEGER,
    remaining_tokens INTEGER,
    retry_after_seconds INTEGER,
    strictness_factor NUMERIC(8,4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT rate_limit_decisions_algorithm_valid CHECK (algorithm IN ('SLIDING_WINDOW', 'TOKEN_BUCKET', 'HYBRID')),
    CONSTRAINT rate_limit_decisions_decision_valid CHECK (decision IN ('ALLOW', 'BLOCK')),
    CONSTRAINT rate_limit_decisions_effective_limit_positive CHECK (effective_limit > 0),
    CONSTRAINT rate_limit_decisions_observed_count_valid CHECK (observed_count IS NULL OR observed_count >= 0),
    CONSTRAINT rate_limit_decisions_remaining_tokens_valid CHECK (remaining_tokens IS NULL OR remaining_tokens >= 0),
    CONSTRAINT rate_limit_decisions_retry_after_valid CHECK (retry_after_seconds IS NULL OR retry_after_seconds >= 0),
    CONSTRAINT rate_limit_decisions_strictness_valid CHECK (strictness_factor >= 0.1000 AND strictness_factor <= 10.0000),
    CONSTRAINT rate_limit_decisions_deleted_after_created CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX ux_rate_limit_decisions_request_log_id_active
    ON rate_limit_decisions (request_log_id)
    WHERE deleted_at IS NULL;
CREATE INDEX ix_rate_limit_decisions_policy_id ON rate_limit_decisions (policy_id);
CREATE INDEX ix_rate_limit_decisions_decision_created ON rate_limit_decisions (decision, created_at DESC);

CREATE TABLE analytics_rollups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bucket_start TIMESTAMPTZ NOT NULL,
    bucket_end TIMESTAMPTZ NOT NULL,
    granularity VARCHAR(20) NOT NULL,
    consumer_id UUID REFERENCES api_consumers (id),
    route_id UUID REFERENCES gateway_routes (id),
    total_requests BIGINT NOT NULL,
    allowed_requests BIGINT NOT NULL,
    blocked_requests BIGINT NOT NULL,
    error_requests BIGINT NOT NULL,
    avg_response_time_ms NUMERIC(12,4),
    p95_response_time_ms NUMERIC(12,4),
    unique_source_ips INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT analytics_rollups_bucket_valid CHECK (bucket_end > bucket_start),
    CONSTRAINT analytics_rollups_granularity_valid CHECK (granularity IN ('MINUTE', 'HOUR', 'DAY')),
    CONSTRAINT analytics_rollups_counts_valid CHECK (
        total_requests >= 0
        AND allowed_requests >= 0
        AND blocked_requests >= 0
        AND error_requests >= 0
        AND unique_source_ips >= 0
        AND total_requests = allowed_requests + blocked_requests + error_requests
    ),
    CONSTRAINT analytics_rollups_latency_valid CHECK (
        (avg_response_time_ms IS NULL OR avg_response_time_ms >= 0)
        AND (p95_response_time_ms IS NULL OR p95_response_time_ms >= 0)
    ),
    CONSTRAINT analytics_rollups_deleted_after_created CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX ux_analytics_rollups_bucket_scope_active
    ON analytics_rollups (
        bucket_start,
        bucket_end,
        granularity,
        COALESCE(consumer_id, '00000000-0000-0000-0000-000000000000'::uuid),
        COALESCE(route_id, '00000000-0000-0000-0000-000000000000'::uuid)
    )
    WHERE deleted_at IS NULL;
CREATE INDEX ix_analytics_rollups_bucket_start ON analytics_rollups (bucket_start DESC);
CREATE INDEX ix_analytics_rollups_consumer_bucket ON analytics_rollups (consumer_id, bucket_start DESC);
CREATE INDEX ix_analytics_rollups_route_bucket ON analytics_rollups (route_id, bucket_start DESC);

CREATE TABLE client_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consumer_id UUID NOT NULL REFERENCES api_consumers (id) ON DELETE CASCADE,
    calculated_at TIMESTAMPTZ NOT NULL,
    window_seconds INTEGER NOT NULL,
    requests_per_second NUMERIC(14,6) NOT NULL,
    blocked_rate NUMERIC(8,6) NOT NULL,
    error_rate NUMERIC(8,6) NOT NULL,
    avg_latency_ms NUMERIC(12,4),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT client_metrics_window_positive CHECK (window_seconds > 0),
    CONSTRAINT client_metrics_rates_valid CHECK (
        requests_per_second >= 0
        AND blocked_rate >= 0 AND blocked_rate <= 1
        AND error_rate >= 0 AND error_rate <= 1
    ),
    CONSTRAINT client_metrics_latency_valid CHECK (avg_latency_ms IS NULL OR avg_latency_ms >= 0),
    CONSTRAINT client_metrics_deleted_after_created CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX ux_client_metrics_consumer_calculated_window_active
    ON client_metrics (consumer_id, calculated_at, window_seconds)
    WHERE deleted_at IS NULL;
CREATE INDEX ix_client_metrics_calculated_at ON client_metrics (calculated_at DESC);

CREATE TABLE route_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    route_id UUID NOT NULL REFERENCES gateway_routes (id) ON DELETE CASCADE,
    calculated_at TIMESTAMPTZ NOT NULL,
    window_seconds INTEGER NOT NULL,
    requests_per_second NUMERIC(14,6) NOT NULL,
    blocked_rate NUMERIC(8,6) NOT NULL,
    error_rate NUMERIC(8,6) NOT NULL,
    avg_latency_ms NUMERIC(12,4),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT route_metrics_window_positive CHECK (window_seconds > 0),
    CONSTRAINT route_metrics_rates_valid CHECK (
        requests_per_second >= 0
        AND blocked_rate >= 0 AND blocked_rate <= 1
        AND error_rate >= 0 AND error_rate <= 1
    ),
    CONSTRAINT route_metrics_latency_valid CHECK (avg_latency_ms IS NULL OR avg_latency_ms >= 0),
    CONSTRAINT route_metrics_deleted_after_created CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX ux_route_metrics_route_calculated_window_active
    ON route_metrics (route_id, calculated_at, window_seconds)
    WHERE deleted_at IS NULL;
CREATE INDEX ix_route_metrics_calculated_at ON route_metrics (calculated_at DESC);

CREATE TABLE traffic_heatmap_buckets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bucket_start TIMESTAMPTZ NOT NULL,
    day_of_week SMALLINT NOT NULL,
    hour_of_day SMALLINT NOT NULL,
    consumer_id UUID REFERENCES api_consumers (id),
    route_id UUID REFERENCES gateway_routes (id),
    request_count BIGINT NOT NULL,
    blocked_count BIGINT NOT NULL,
    avg_latency_ms NUMERIC(12,4),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT traffic_heatmap_day_valid CHECK (day_of_week BETWEEN 0 AND 6),
    CONSTRAINT traffic_heatmap_hour_valid CHECK (hour_of_day BETWEEN 0 AND 23),
    CONSTRAINT traffic_heatmap_counts_valid CHECK (request_count >= 0 AND blocked_count >= 0 AND blocked_count <= request_count),
    CONSTRAINT traffic_heatmap_latency_valid CHECK (avg_latency_ms IS NULL OR avg_latency_ms >= 0),
    CONSTRAINT traffic_heatmap_deleted_after_created CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE UNIQUE INDEX ux_traffic_heatmap_scope_active
    ON traffic_heatmap_buckets (
        bucket_start,
        day_of_week,
        hour_of_day,
        COALESCE(consumer_id, '00000000-0000-0000-0000-000000000000'::uuid),
        COALESCE(route_id, '00000000-0000-0000-0000-000000000000'::uuid)
    )
    WHERE deleted_at IS NULL;
CREATE INDEX ix_traffic_heatmap_bucket_start ON traffic_heatmap_buckets (bucket_start DESC);

CREATE TABLE anomaly_stat_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    consumer_id UUID REFERENCES api_consumers (id),
    route_id UUID REFERENCES gateway_routes (id),
    metric_name VARCHAR(80) NOT NULL,
    window_seconds INTEGER NOT NULL,
    sample_count INTEGER NOT NULL,
    rolling_mean NUMERIC(18,6) NOT NULL,
    rolling_variance NUMERIC(18,6) NOT NULL,
    rolling_stddev NUMERIC(18,6) NOT NULL,
    rolling_z_score NUMERIC(18,6) NOT NULL,
    ema_value NUMERIC(18,6) NOT NULL,
    adaptive_threshold NUMERIC(18,6) NOT NULL,
    calculated_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT anomaly_stat_snapshots_metric_not_blank CHECK (length(trim(metric_name)) > 0),
    CONSTRAINT anomaly_stat_snapshots_window_positive CHECK (window_seconds > 0),
    CONSTRAINT anomaly_stat_snapshots_sample_count_valid CHECK (sample_count >= 0),
    CONSTRAINT anomaly_stat_snapshots_stats_valid CHECK (
        rolling_variance >= 0
        AND rolling_stddev >= 0
        AND adaptive_threshold >= 0
    ),
    CONSTRAINT anomaly_stat_snapshots_deleted_after_created CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE INDEX ix_anomaly_stat_snapshots_scope_calculated
    ON anomaly_stat_snapshots (consumer_id, route_id, metric_name, calculated_at DESC);

CREATE TABLE anomaly_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    stats_snapshot_id UUID NOT NULL REFERENCES anomaly_stat_snapshots (id),
    consumer_id UUID REFERENCES api_consumers (id),
    route_id UUID REFERENCES gateway_routes (id),
    severity VARCHAR(40) NOT NULL,
    metric_name VARCHAR(80) NOT NULL,
    observed_value NUMERIC(18,6) NOT NULL,
    threshold_value NUMERIC(18,6) NOT NULL,
    z_score NUMERIC(18,6) NOT NULL,
    status VARCHAR(40) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT anomaly_records_severity_valid CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT anomaly_records_metric_not_blank CHECK (length(trim(metric_name)) > 0),
    CONSTRAINT anomaly_records_threshold_valid CHECK (threshold_value >= 0),
    CONSTRAINT anomaly_records_status_valid CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED')),
    CONSTRAINT anomaly_records_description_not_blank CHECK (length(trim(description)) > 0),
    CONSTRAINT anomaly_records_deleted_after_created CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE INDEX ix_anomaly_records_consumer_created ON anomaly_records (consumer_id, created_at DESC);
CREATE INDEX ix_anomaly_records_route_created ON anomaly_records (route_id, created_at DESC);
CREATE INDEX ix_anomaly_records_status_severity ON anomaly_records (status, severity);

CREATE TABLE rate_limit_adjustments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id UUID NOT NULL REFERENCES rate_limit_policies (id),
    anomaly_record_id UUID REFERENCES anomaly_records (id),
    consumer_id UUID REFERENCES api_consumers (id),
    route_id UUID REFERENCES gateway_routes (id),
    previous_strictness_factor NUMERIC(8,4) NOT NULL,
    new_strictness_factor NUMERIC(8,4) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT rate_limit_adjustments_previous_valid CHECK (previous_strictness_factor >= 0.1000 AND previous_strictness_factor <= 10.0000),
    CONSTRAINT rate_limit_adjustments_new_valid CHECK (new_strictness_factor >= 0.1000 AND new_strictness_factor <= 10.0000),
    CONSTRAINT rate_limit_adjustments_reason_not_blank CHECK (length(trim(reason)) > 0),
    CONSTRAINT rate_limit_adjustments_deleted_after_created CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE INDEX ix_rate_limit_adjustments_policy_created ON rate_limit_adjustments (policy_id, created_at DESC);
CREATE INDEX ix_rate_limit_adjustments_anomaly_record_id ON rate_limit_adjustments (anomaly_record_id);

CREATE TABLE alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    anomaly_record_id UUID REFERENCES anomaly_records (id),
    consumer_id UUID REFERENCES api_consumers (id),
    route_id UUID REFERENCES gateway_routes (id),
    alert_type VARCHAR(80) NOT NULL,
    severity VARCHAR(40) NOT NULL,
    title VARCHAR(180) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    status VARCHAR(40) NOT NULL,
    acknowledged_by_user_id UUID REFERENCES app_users (id),
    acknowledged_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT alerts_type_not_blank CHECK (length(trim(alert_type)) > 0),
    CONSTRAINT alerts_severity_valid CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT alerts_title_not_blank CHECK (length(trim(title)) > 0),
    CONSTRAINT alerts_message_not_blank CHECK (length(trim(message)) > 0),
    CONSTRAINT alerts_status_valid CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED')),
    CONSTRAINT alerts_acknowledgement_consistent CHECK (
        (status = 'ACKNOWLEDGED' AND acknowledged_by_user_id IS NOT NULL AND acknowledged_at IS NOT NULL)
        OR status <> 'ACKNOWLEDGED'
    ),
    CONSTRAINT alerts_resolved_consistent CHECK (
        (status = 'RESOLVED' AND resolved_at IS NOT NULL)
        OR status <> 'RESOLVED'
    ),
    CONSTRAINT alerts_deleted_after_created CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE INDEX ix_alerts_status_severity_created ON alerts (status, severity, created_at DESC);
CREATE INDEX ix_alerts_consumer_created ON alerts (consumer_id, created_at DESC);
CREATE INDEX ix_alerts_route_created ON alerts (route_id, created_at DESC);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id UUID REFERENCES app_users (id),
    action VARCHAR(120) NOT NULL,
    resource_type VARCHAR(120) NOT NULL,
    resource_id UUID,
    correlation_id UUID,
    ip_address INET,
    user_agent TEXT,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT audit_logs_action_not_blank CHECK (length(trim(action)) > 0),
    CONSTRAINT audit_logs_resource_type_not_blank CHECK (length(trim(resource_type)) > 0),
    CONSTRAINT audit_logs_metadata_object CHECK (jsonb_typeof(metadata) = 'object'),
    CONSTRAINT audit_logs_deleted_after_created CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE INDEX ix_audit_logs_actor_created ON audit_logs (actor_user_id, created_at DESC);
CREATE INDEX ix_audit_logs_resource ON audit_logs (resource_type, resource_id);
CREATE INDEX ix_audit_logs_correlation_id ON audit_logs (correlation_id);

CREATE TABLE report_exports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requested_by_user_id UUID NOT NULL REFERENCES app_users (id),
    report_type VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL,
    filter_hash VARCHAR(128) NOT NULL,
    storage_object_key VARCHAR(1024),
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    error_message VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT report_exports_type_not_blank CHECK (length(trim(report_type)) > 0),
    CONSTRAINT report_exports_status_valid CHECK (status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'EXPIRED')),
    CONSTRAINT report_exports_filter_hash_not_blank CHECK (length(trim(filter_hash)) > 0),
    CONSTRAINT report_exports_completed_after_started CHECK (completed_at IS NULL OR completed_at >= started_at),
    CONSTRAINT report_exports_expires_after_completed CHECK (expires_at IS NULL OR completed_at IS NULL OR expires_at > completed_at),
    CONSTRAINT report_exports_error_consistent CHECK (
        (status = 'FAILED' AND error_message IS NOT NULL AND length(trim(error_message)) > 0)
        OR status <> 'FAILED'
    ),
    CONSTRAINT report_exports_deleted_after_created CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE INDEX ix_report_exports_requested_by_created ON report_exports (requested_by_user_id, created_at DESC);
CREATE INDEX ix_report_exports_status ON report_exports (status);

CREATE TABLE kafka_event_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(140) NOT NULL,
    aggregate_type VARCHAR(120) NOT NULL,
    aggregate_id UUID,
    correlation_id UUID,
    source_service VARCHAR(120) NOT NULL,
    schema_version INTEGER NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(40) NOT NULL,
    attempts INTEGER NOT NULL,
    next_attempt_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT kafka_event_outbox_event_type_not_blank CHECK (length(trim(event_type)) > 0),
    CONSTRAINT kafka_event_outbox_aggregate_type_not_blank CHECK (length(trim(aggregate_type)) > 0),
    CONSTRAINT kafka_event_outbox_source_service_not_blank CHECK (length(trim(source_service)) > 0),
    CONSTRAINT kafka_event_outbox_schema_version_positive CHECK (schema_version > 0),
    CONSTRAINT kafka_event_outbox_payload_object CHECK (jsonb_typeof(payload) = 'object'),
    CONSTRAINT kafka_event_outbox_status_valid CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT kafka_event_outbox_attempts_valid CHECK (attempts >= 0),
    CONSTRAINT kafka_event_outbox_published_consistent CHECK (
        (status = 'PUBLISHED' AND published_at IS NOT NULL)
        OR status <> 'PUBLISHED'
    ),
    CONSTRAINT kafka_event_outbox_deleted_after_created CHECK (deleted_at IS NULL OR deleted_at >= created_at)
);

CREATE INDEX ix_kafka_event_outbox_status_next_attempt ON kafka_event_outbox (status, next_attempt_at);
CREATE INDEX ix_kafka_event_outbox_event_type_created ON kafka_event_outbox (event_type, created_at DESC);
CREATE INDEX ix_kafka_event_outbox_correlation_id ON kafka_event_outbox (correlation_id);

DO $$
DECLARE
    target_table TEXT;
BEGIN
    FOREACH target_table IN ARRAY ARRAY[
        'roles',
        'app_users',
        'refresh_tokens',
        'login_audit_logs',
        'api_consumers',
        'api_consumer_credentials',
        'upstream_services',
        'gateway_routes',
        'gateway_route_predicates',
        'rate_limit_policies',
        'rate_limit_assignments',
        'request_logs',
        'rate_limit_decisions',
        'analytics_rollups',
        'client_metrics',
        'route_metrics',
        'traffic_heatmap_buckets',
        'anomaly_stat_snapshots',
        'anomaly_records',
        'rate_limit_adjustments',
        'alerts',
        'audit_logs',
        'report_exports',
        'kafka_event_outbox'
    ]
    LOOP
        EXECUTE format(
            'CREATE TRIGGER %I BEFORE UPDATE ON %I FOR EACH ROW EXECUTE FUNCTION set_updated_at()',
            'trg_' || target_table || '_set_updated_at',
            target_table
        );
    END LOOP;
END;
$$;

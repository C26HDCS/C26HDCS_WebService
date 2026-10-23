CREATE TABLE IF NOT EXISTS tb_user (
    user_id VARCHAR(50) PRIMARY KEY, password VARCHAR(100) NOT NULL, user_name VARCHAR(100),
    email VARCHAR(255), phone_number VARCHAR(30), role_type VARCHAR(20) DEFAULT 'USER',
    use_yn CHAR(1) DEFAULT 'Y', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS tb_observation_station (
    station_id BIGSERIAL PRIMARY KEY, name VARCHAR(100), obs_code VARCHAR(50),
    lat DOUBLE PRECISION, lng DOUBLE PRECISION, address VARCHAR(500), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS tb_device_group (
    group_id BIGSERIAL PRIMARY KEY, group_name VARCHAR(100), sort_order INTEGER DEFAULT 0,
    use_yn CHAR(1) DEFAULT 'Y', updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS tb_device (
    device_id BIGSERIAL PRIMARY KEY, device_name VARCHAR(100), device_type VARCHAR(100), ip_address VARCHAR(100),
    device_status VARCHAR(20) DEFAULT 'NORMAL', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_received_at TIMESTAMP, last_receive_status VARCHAR(20), station_id BIGINT, group_id BIGINT, nms_device_name VARCHAR(100));
CREATE TABLE IF NOT EXISTS tb_device_status (
    status_id BIGSERIAL PRIMARY KEY, device_id BIGINT REFERENCES tb_device,
    device_status VARCHAR(20), status_message VARCHAR(500), checked_at TIMESTAMP);
CREATE TABLE IF NOT EXISTS tb_log (
    id BIGSERIAL PRIMARY KEY, device_id BIGINT NOT NULL REFERENCES tb_device,
    level VARCHAR(10), message VARCHAR(1000), reg_dt TIMESTAMP);
CREATE TABLE IF NOT EXISTS tb_event (
    id BIGSERIAL PRIMARY KEY, user_id VARCHAR(50) REFERENCES tb_user ON DELETE SET NULL,
    actor_user_id VARCHAR(50), record_key VARCHAR(36) UNIQUE, event_type VARCHAR(30), message VARCHAR(1000), reg_dt TIMESTAMP);
CREATE TABLE IF NOT EXISTS tb_alarm (
    alarm_id BIGSERIAL PRIMARY KEY, device_id BIGINT REFERENCES tb_device,
    alarm_type VARCHAR(50), alarm_level VARCHAR(20), alarm_message VARCHAR(500), occurred_at TIMESTAMP);
CREATE TABLE IF NOT EXISTS tb_alarm_check (
    alarm_id BIGINT REFERENCES tb_alarm, user_id VARCHAR(50) REFERENCES tb_user,
    checked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY(alarm_id,user_id));
CREATE TABLE IF NOT EXISTS tb_report (
    report_id BIGSERIAL PRIMARY KEY, user_id VARCHAR(50) REFERENCES tb_user, report_name VARCHAR(255),
    generate_type VARCHAR(20), period_from DATE, period_to DATE, data_types VARCHAR(100),
    file_name VARCHAR(255), file_path VARCHAR(500), file_size BIGINT, reg_dt TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE IF NOT EXISTS tb_python_alarm_state (
    device_id BIGINT REFERENCES tb_device, incident_key VARCHAR(64), active BOOLEAN,
    updated_at TIMESTAMP, PRIMARY KEY(device_id,incident_key));
CREATE TABLE IF NOT EXISTS tb_python_device_mapping (
    data_kind VARCHAR(3), external_key VARCHAR(255), device_id BIGINT REFERENCES tb_device,
    enabled BOOLEAN DEFAULT TRUE, PRIMARY KEY(data_kind,external_key));

-- Apply once before deploying Python telemetry. No existing data is deleted.
-- tb_log and tb_alarm already belong to WebService.
CREATE TABLE IF NOT EXISTS public.tb_python_alarm_state (
    device_id BIGINT NOT NULL REFERENCES public.tb_device(device_id),
    incident_key VARCHAR(64) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL,
    PRIMARY KEY (device_id, incident_key)
);

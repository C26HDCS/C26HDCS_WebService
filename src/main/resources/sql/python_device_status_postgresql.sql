BEGIN;

CREATE TABLE IF NOT EXISTS public.tb_python_component_status (
    device_id BIGINT NOT NULL REFERENCES public.tb_device(device_id),
    data_kind VARCHAR(3) NOT NULL CHECK (data_kind IN ('DCS', 'KA')),
    component VARCHAR(40) NOT NULL,
    scope VARCHAR(255) NOT NULL DEFAULT '',
    status VARCHAR(20) NOT NULL CHECK (status IN ('NORMAL', 'WARNING', 'ERROR')),
    detail VARCHAR(500) NOT NULL,
    checked_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ,
    PRIMARY KEY (device_id, data_kind, component, scope)
);
CREATE INDEX IF NOT EXISTS idx_python_component_expiry ON public.tb_python_component_status(expires_at);

CREATE OR REPLACE VIEW public.v_python_component_health AS
SELECT s.device_id,s.data_kind,s.component,s.scope,s.status,s.detail,s.checked_at,s.expires_at,
       CASE WHEN expires_at IS NOT NULL AND expires_at < CURRENT_TIMESTAMP THEN 'ERROR' ELSE status END AS effective_status,
       (expires_at IS NOT NULL AND expires_at < CURRENT_TIMESTAMP) AS expired
FROM public.tb_python_component_status s;

CREATE OR REPLACE FUNCTION public.hdcs_refresh_python_device_status(p_device BIGINT)
RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE
    old_status TEXT;
    new_status TEXT;
    reason TEXT;
    timed_out BOOLEAN;
    previous_timeout BOOLEAN;
    local_now TIMESTAMP := CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul';
    timeout_key TEXT := md5('WEB_PYTHON_STATUS_TIMEOUT');
BEGIN
    SELECT device_status INTO old_status FROM public.tb_device WHERE device_id = p_device FOR NO KEY UPDATE;
    IF NOT FOUND THEN RETURN; END IF;
    SELECT CASE MAX(CASE effective_status WHEN 'ERROR' THEN 2 WHEN 'WARNING' THEN 1 ELSE 0 END)
               WHEN 2 THEN 'ERROR' WHEN 1 THEN 'WARNING' WHEN 0 THEN 'NORMAL' END,
           LEFT(string_agg(component || '[' || scope || ']=' || effective_status ||
               CASE WHEN expired THEN '(heartbeat timeout)' ELSE ':' || detail END, '; ' ORDER BY component, scope), 500),
           COALESCE(bool_or(expired), FALSE)
    INTO new_status, reason, timed_out
    FROM public.v_python_component_health WHERE device_id = p_device;
    IF new_status IS NULL THEN RETURN; END IF;
    IF old_status IS DISTINCT FROM new_status THEN
        UPDATE public.tb_device SET device_status = new_status WHERE device_id = p_device;
        INSERT INTO public.tb_device_status(device_id, device_status, status_message, checked_at)
        VALUES(p_device, new_status, reason, local_now);
    END IF;
    INSERT INTO public.tb_python_alarm_state(device_id, incident_key, active, updated_at)
    VALUES(p_device, timeout_key, FALSE, local_now) ON CONFLICT DO NOTHING;
    SELECT active INTO previous_timeout FROM public.tb_python_alarm_state
    WHERE device_id = p_device AND incident_key = timeout_key FOR UPDATE;
    IF previous_timeout IS DISTINCT FROM timed_out THEN
        IF timed_out THEN
            INSERT INTO public.tb_alarm(device_id, alarm_type, alarm_level, alarm_message, occurred_at)
            VALUES(p_device, '시스템', 'HIGH', '[Python:STATUS_TIMEOUT] ' || LEFT(reason, 470), local_now);
        END IF;
        INSERT INTO public.tb_log(device_id, level, message, reg_dt)
        VALUES(p_device, CASE WHEN timed_out THEN 'ERROR' ELSE 'INFO' END,
               CASE WHEN timed_out THEN '[Python:STATUS_TIMEOUT] ' ELSE '[Python:STATUS_RECOVERED] ' END || reason, local_now);
        UPDATE public.tb_python_alarm_state SET active = timed_out, updated_at = local_now
        WHERE device_id = p_device AND incident_key = timeout_key;
    END IF;
END;
$$;

-- Expect a station/camera monitor even before its first successful check.
INSERT INTO public.tb_python_component_status
    (device_id, data_kind, component, scope, status, detail, checked_at, expires_at)
SELECT device_id, data_kind, 'MONITOR', external_key, 'WARNING', 'Waiting for first monitor check',
       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '120 seconds'
FROM public.tb_python_device_mapping WHERE enabled = TRUE
ON CONFLICT DO NOTHING;

COMMIT;

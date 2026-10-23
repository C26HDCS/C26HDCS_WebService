BEGIN;

ALTER TABLE public.tb_python_component_status ADD COLUMN IF NOT EXISTS owner_key VARCHAR(255);
ALTER TABLE public.tb_python_component_status ADD COLUMN IF NOT EXISTS destination_id VARCHAR(255);
ALTER TABLE public.tb_python_component_status ADD COLUMN IF NOT EXISTS incident_key VARCHAR(64);
CREATE TABLE IF NOT EXISTS public.tb_python_delivery_target (
    data_kind VARCHAR(3) NOT NULL CHECK(data_kind IN ('DCS','KA')),
    destination_id VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY(data_kind,destination_id)
);

-- Infer ownership for old rows only when there is one unambiguous mapping.
UPDATE public.tb_python_component_status s SET owner_key = scope
WHERE component='MONITOR' AND owner_key IS NULL;
UPDATE public.tb_python_component_status s SET owner_key=scope
WHERE owner_key IS NULL AND EXISTS(SELECT 1 FROM public.tb_python_device_mapping m
    WHERE m.data_kind=s.data_kind AND m.external_key=s.scope);
UPDATE public.tb_python_component_status s SET owner_key = (
    SELECT MIN(m.external_key) FROM public.tb_python_device_mapping m
    WHERE m.device_id=s.device_id AND m.data_kind=s.data_kind AND m.enabled=TRUE)
WHERE s.owner_key IS NULL AND (SELECT COUNT(*) FROM public.tb_python_device_mapping m
    WHERE m.device_id=s.device_id AND m.data_kind=s.data_kind AND m.enabled=TRUE)=1;
UPDATE public.tb_python_component_status SET incident_key=encode(sha256(convert_to(component || ':' || scope,'UTF8')),'hex')
WHERE incident_key IS NULL AND component NOT IN ('MONITOR','DELIVERY');
UPDATE public.tb_python_component_status SET destination_id=scope
WHERE component IN ('DCS_SEND_ERROR','KA_SEND_ERROR') AND destination_id IS NULL;
UPDATE public.tb_python_component_status SET destination_id=substring(scope FROM position('|' IN scope)+1)
WHERE component IN ('DCS_SENDER_RUNTIME','KA_SENDER_RUNTIME') AND destination_id IS NULL AND position('|' IN scope)>0;
INSERT INTO public.tb_python_delivery_target(data_kind,destination_id)
SELECT DISTINCT data_kind,destination_id FROM public.tb_python_component_status WHERE destination_id IS NOT NULL
ON CONFLICT DO NOTHING;

CREATE OR REPLACE VIEW public.v_python_component_health AS
SELECT s.device_id,s.data_kind,s.component,s.scope,s.status,s.detail,s.checked_at,s.expires_at,
       CASE WHEN expires_at IS NOT NULL AND expires_at<CURRENT_TIMESTAMP THEN 'ERROR' ELSE status END AS effective_status,
       (expires_at IS NOT NULL AND expires_at<CURRENT_TIMESTAMP) AS expired
FROM public.tb_python_component_status s
WHERE EXISTS (SELECT 1 FROM public.tb_python_device_mapping m
    WHERE m.device_id=s.device_id AND m.data_kind=s.data_kind AND m.enabled=TRUE
      AND (s.owner_key IS NULL OR m.external_key=s.owner_key))
AND (s.destination_id IS NULL OR EXISTS (SELECT 1 FROM public.tb_python_delivery_target d
    WHERE d.data_kind=s.data_kind AND d.destination_id=s.destination_id AND d.enabled=TRUE));

CREATE OR REPLACE FUNCTION public.hdcs_prune_python_component_status()
RETURNS INTEGER LANGUAGE plpgsql AS $$
DECLARE d BIGINT; removed INTEGER:=0; affected INTEGER; old_state TEXT; item RECORD;
BEGIN
    FOR d IN SELECT DISTINCT s.device_id FROM public.tb_python_component_status s
        WHERE NOT EXISTS(SELECT 1 FROM public.tb_python_device_mapping m
            WHERE m.device_id=s.device_id AND m.data_kind=s.data_kind AND m.enabled=TRUE
              AND (s.owner_key IS NULL OR m.external_key=s.owner_key))
        OR (s.destination_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM public.tb_python_delivery_target t
            WHERE t.data_kind=s.data_kind AND t.destination_id=s.destination_id AND t.enabled=TRUE))
        ORDER BY s.device_id
    LOOP
        SELECT device_status INTO old_state FROM public.tb_device WHERE device_id=d FOR NO KEY UPDATE;
        FOR item IN DELETE FROM public.tb_python_component_status s WHERE s.device_id=d AND (
            NOT EXISTS(SELECT 1 FROM public.tb_python_device_mapping m
                WHERE m.device_id=s.device_id AND m.data_kind=s.data_kind AND m.enabled=TRUE
                  AND (s.owner_key IS NULL OR m.external_key=s.owner_key))
            OR (s.destination_id IS NOT NULL AND NOT EXISTS(SELECT 1 FROM public.tb_python_delivery_target t
                WHERE t.data_kind=s.data_kind AND t.destination_id=s.destination_id AND t.enabled=TRUE)))
            RETURNING incident_key
        LOOP
            removed:=removed+1;
            UPDATE public.tb_python_alarm_state SET active=FALSE,updated_at=CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'
            WHERE device_id=d AND incident_key=item.incident_key;
        END LOOP;
        IF EXISTS(SELECT 1 FROM public.v_python_component_health WHERE device_id=d) THEN
            PERFORM public.hdcs_refresh_python_device_status(d);
        ELSE
            -- No assignment is not proof of healthy hardware; show WARNING, not a false ERROR/NORMAL.
            IF old_state IS DISTINCT FROM 'WARNING' THEN
                UPDATE public.tb_device SET device_status='WARNING' WHERE device_id=d;
                INSERT INTO public.tb_device_status(device_id,device_status,status_message,checked_at)
                VALUES(d,'WARNING','[Python] Monitoring assignment removed',CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul');
            END IF;
            UPDATE public.tb_python_alarm_state SET active=FALSE,updated_at=CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'
            WHERE device_id=d AND incident_key=md5('WEB_PYTHON_STATUS_TIMEOUT');
        END IF;
    END LOOP;
    RETURN removed;
END;
$$;

CREATE OR REPLACE FUNCTION public.hdcs_python_mapping_changed()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP <> 'DELETE' AND NEW.enabled=TRUE THEN
        INSERT INTO public.tb_python_component_status
          (device_id,data_kind,component,scope,status,detail,checked_at,expires_at,owner_key)
        VALUES(NEW.device_id,NEW.data_kind,'MONITOR',NEW.external_key,'WARNING','Waiting for first monitor check',
               CURRENT_TIMESTAMP,CURRENT_TIMESTAMP+INTERVAL '120 seconds',NEW.external_key) ON CONFLICT DO NOTHING;
    END IF;
    PERFORM public.hdcs_prune_python_component_status();
    RETURN NULL;
END;
$$;
CREATE OR REPLACE FUNCTION public.hdcs_python_destination_changed()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    PERFORM public.hdcs_prune_python_component_status();
    RETURN NULL;
END;
$$;
DROP TRIGGER IF EXISTS trg_python_mapping_status ON public.tb_python_device_mapping;
CREATE TRIGGER trg_python_mapping_status AFTER INSERT OR UPDATE OR DELETE ON public.tb_python_device_mapping
FOR EACH ROW EXECUTE FUNCTION public.hdcs_python_mapping_changed();
DROP TRIGGER IF EXISTS trg_python_destination_status ON public.tb_python_delivery_target;
CREATE TRIGGER trg_python_destination_status AFTER UPDATE OR DELETE ON public.tb_python_delivery_target
FOR EACH ROW EXECUTE FUNCTION public.hdcs_python_destination_changed();
SELECT public.hdcs_prune_python_component_status();
COMMIT;

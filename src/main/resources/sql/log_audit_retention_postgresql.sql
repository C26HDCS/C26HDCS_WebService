-- Apply to the existing DB before deploying this version. Does not delete records.
BEGIN;
ALTER TABLE public.tb_event ADD COLUMN IF NOT EXISTS actor_user_id VARCHAR(50);
ALTER TABLE public.tb_event ADD COLUMN IF NOT EXISTS record_key VARCHAR(36);
UPDATE public.tb_event SET actor_user_id = user_id WHERE actor_user_id IS NULL;
ALTER TABLE public.tb_event ALTER COLUMN user_id DROP NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_tb_event_record_key ON public.tb_event(record_key);

-- Keep historical actor text even if the linked user is later removed.
DO $$
DECLARE fk RECORD;
BEGIN
    FOR fk IN
        SELECT c.conname FROM pg_constraint c
        JOIN pg_attribute a ON a.attrelid=c.conrelid AND a.attnum=ANY(c.conkey)
        WHERE c.conrelid='public.tb_event'::regclass AND c.contype='f'
          AND a.attname='user_id' AND c.confrelid='public.tb_user'::regclass
    LOOP
        EXECUTE format('ALTER TABLE public.tb_event DROP CONSTRAINT %I', fk.conname);
    END LOOP;
    ALTER TABLE public.tb_event ADD CONSTRAINT fk_tb_event_user
        FOREIGN KEY(user_id) REFERENCES public.tb_user(user_id) ON DELETE SET NULL;
END $$;

CREATE INDEX IF NOT EXISTS idx_tb_log_retention ON public.tb_log(reg_dt);
CREATE INDEX IF NOT EXISTS idx_tb_event_retention ON public.tb_event(reg_dt);

-- Retain at least five calendar years, including the complete cutoff date.
CREATE OR REPLACE FUNCTION public.hdcs_protect_log_retention() RETURNS TRIGGER
LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP='TRUNCATE' THEN
        RAISE EXCEPTION 'TRUNCATE is prohibited for retained HDCS audit/log records';
    END IF;
    IF OLD.reg_dt >= ((CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul')::date - INTERVAL '5 years') THEN
        IF TG_OP='UPDATE' AND to_jsonb(NEW)=to_jsonb(OLD) THEN RETURN NEW; END IF;
        IF TG_OP='UPDATE' AND TG_TABLE_NAME='tb_event'
           AND to_jsonb(NEW)->'user_id'='null'::jsonb
           AND (to_jsonb(NEW)-'user_id')=(to_jsonb(OLD)-'user_id') THEN
            RETURN NEW;
        END IF;
        RAISE EXCEPTION 'HDCS log/event record is within the five-year retention period';
    END IF;
    IF TG_OP='UPDATE' THEN RETURN NEW; END IF;
    RETURN OLD;
END $$;

DROP TRIGGER IF EXISTS trg_tb_log_retention ON public.tb_log;
CREATE TRIGGER trg_tb_log_retention BEFORE DELETE OR UPDATE ON public.tb_log
    FOR EACH ROW EXECUTE FUNCTION public.hdcs_protect_log_retention();
DROP TRIGGER IF EXISTS trg_tb_event_retention ON public.tb_event;
CREATE TRIGGER trg_tb_event_retention BEFORE DELETE OR UPDATE ON public.tb_event
    FOR EACH ROW EXECUTE FUNCTION public.hdcs_protect_log_retention();
DROP TRIGGER IF EXISTS trg_tb_log_no_truncate ON public.tb_log;
CREATE TRIGGER trg_tb_log_no_truncate BEFORE TRUNCATE ON public.tb_log
    FOR EACH STATEMENT EXECUTE FUNCTION public.hdcs_protect_log_retention();
DROP TRIGGER IF EXISTS trg_tb_event_no_truncate ON public.tb_event;
CREATE TRIGGER trg_tb_event_no_truncate BEFORE TRUNCATE ON public.tb_event
    FOR EACH STATEMENT EXECUTE FUNCTION public.hdcs_protect_log_retention();
COMMIT;

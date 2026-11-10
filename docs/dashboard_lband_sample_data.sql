-- c26hdcs에서 파일 전체를 실행합니다.
-- preview: 조회 / insert: 예시 등록 / remove: 이 파일이 넣은 예시 제거
BEGIN;
SET LOCAL lock_timeout = '5s';

CREATE TEMP TABLE lband_sample_options ON COMMIT DROP AS
SELECT 'preview'::text AS sample_mode;

DO $check$
BEGIN
    IF current_database() <> 'c26hdcs' THEN
        RAISE EXCEPTION 'Run this script in c26hdcs.';
    END IF;
    IF (SELECT sample_mode FROM lband_sample_options) NOT IN ('preview', 'insert', 'remove') THEN
        RAISE EXCEPTION 'Expected preview, insert, or remove.';
    END IF;
    IF (SELECT sample_mode FROM lband_sample_options) <> 'preview' THEN
        LOCK TABLE public.tb_dcs_data IN SHARE ROW EXCLUSIVE MODE;
    END IF;
END
$check$;

CREATE TEMP TABLE lband_sample_plan ON COMMIT DROP AS
WITH samples(obs_code, sample_kind, quantity_1, frame_hex) AS (
    VALUES
        ('33014010', '강수량', '0012', '023031343435363135313239303030313235304630303036303034343530303030780D'),
        ('33014020', '강수량', '0028', '023031343435363135313239303030323835304630303036303034343530303030710D'),
        ('3203690',  '수위',   '0142', '0230313434353631353132393030313432353046303030363030343435303030307C0D'),
        ('3301612',  '수위',   '0196', '023031343435363135313239303031393635304630303036303034343530303030750D')
)
SELECT s.obs_code, s.sample_kind, d.device_type, s.quantity_1, d.device_id, d.device_name, s.frame_hex,
       '[예시] ' || s.frame_hex AS data_payload
FROM samples s
LEFT JOIN public.tb_observation_station o ON btrim(o.obs_code) = s.obs_code
LEFT JOIN public.tb_device d ON d.station_id = o.station_id
    AND (regexp_replace(lower(d.device_type), '[[:space:]]+', '', 'g') IN ('l대역', 'l대역dcs')
         OR btrim(d.device_type) = s.sample_kind);

CREATE TEMP VIEW lband_sample_targets AS
SELECT p.*, r.dcs_data_id, r.received_at,
       count(*) OVER (PARTITION BY p.obs_code) AS matching_rows
FROM lband_sample_plan p
JOIN public.tb_dcs_data r ON r.device_id = p.device_id
    AND r.data_payload IN (p.data_payload, p.frame_hex);

SELECT p.obs_code, p.sample_kind, p.device_type, p.device_id, p.device_name,
       '12.50' AS voltage, p.quantity_1, '0060' AS quantity_2,
       '0445' AS quantity_3, '0000' AS quantity_4,
       CASE
           WHEN p.device_id IS NULL THEN 'DEVICE_NOT_FOUND'
           WHEN count(*) OVER (PARTITION BY p.obs_code) <> 1 THEN 'AMBIGUOUS_DEVICE'
           WHEN EXISTS (
               SELECT FROM lband_sample_targets t WHERE t.obs_code = p.obs_code AND t.matching_rows > 1
           ) THEN 'AMBIGUOUS_PAYLOAD'
           WHEN EXISTS (
               SELECT FROM lband_sample_targets t WHERE t.obs_code = p.obs_code
           ) THEN CASE WHEN EXISTS (
               SELECT FROM lband_sample_targets t WHERE t.obs_code = p.obs_code
                 AND NOT EXISTS (
                     SELECT FROM public.tb_dcs_receive_details detail
                     WHERE detail.dcs_data_id = t.dcs_data_id
                 )
           ) THEN 'ADD_DETAILS' ELSE 'SAMPLE_EXISTS' END
           WHEN EXISTS (
               SELECT FROM public.tb_dcs_data r WHERE r.device_id = p.device_id
           ) THEN 'SKIP_EXISTING_DATA'
           ELSE 'READY'
       END AS action
FROM lband_sample_plan p
ORDER BY p.sample_kind, p.obs_code;

DO $apply$
DECLARE
    mode text;
    affected bigint;
    details_affected bigint;
BEGIN
    SELECT sample_mode INTO mode FROM lband_sample_options;
    IF mode = 'preview' THEN
        RETURN;
    END IF;

    IF EXISTS (
        SELECT FROM lband_sample_plan GROUP BY obs_code
        HAVING count(*) <> 1 OR count(device_id) <> 1
    ) THEN
        RAISE EXCEPTION 'Each sample must match exactly one registered device.';
    END IF;

    IF mode = 'insert' THEN
        INSERT INTO public.tb_dcs_data (device_id, data_payload, received_status, received_at)
        SELECT p.device_id, p.data_payload, 'SUCCESS', CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul'
        FROM lband_sample_plan p
        WHERE NOT EXISTS (SELECT FROM public.tb_dcs_data r WHERE r.device_id = p.device_id);
        GET DIAGNOSTICS affected = ROW_COUNT;

        -- 수신 모듈과 같은 파싱 상태를 쓰고, 출처로 예시 데이터를 구분합니다.
        INSERT INTO public.tb_dcs_receive_details (
            dcs_data_id, received_at, source, raw_hex, parse_status,
            station_number, system_group_number, group_number, field_8_9, voltage,
            parity_1, field_13, quantity_1, parity_2, field_21, quantity_2,
            quantity_3, quantity_4, error_code_hex, document_xor_hex, document_xor_matches
        )
        SELECT p.dcs_data_id, p.received_at AT TIME ZONE 'Asia/Seoul',
               'dashboard-lband-sample', p.frame_hex, 'PARSED',
               '01', '44', '56', '15', '12.50',
               '9', '0', p.quantity_1, 'F', '0', '0060',
               '0445', '0000', substr(p.frame_hex, 67, 2), substr(p.frame_hex, 67, 2), TRUE
        FROM lband_sample_targets p
        WHERE p.matching_rows = 1 AND NOT EXISTS (
            SELECT FROM public.tb_dcs_receive_details detail WHERE detail.dcs_data_id = p.dcs_data_id
        );
        GET DIAGNOSTICS details_affected = ROW_COUNT;
    ELSE
        DELETE FROM public.tb_dcs_receive_details detail
        USING public.tb_dcs_data r, lband_sample_plan p
        WHERE detail.dcs_data_id = r.dcs_data_id AND detail.source = 'dashboard-lband-sample'
          AND r.device_id = p.device_id AND r.data_payload = p.data_payload;
        GET DIAGNOSTICS details_affected = ROW_COUNT;

        DELETE FROM public.tb_dcs_data r
        USING lband_sample_plan p
        WHERE r.device_id = p.device_id AND r.data_payload = p.data_payload;
        GET DIAGNOSTICS affected = ROW_COUNT;
    END IF;
    RAISE NOTICE 'L-band dashboard samples %: % raw row(s), % detail row(s).',
        mode, affected, details_affected;
END
$apply$;
DROP VIEW lband_sample_targets;
COMMIT;

-- c26hdcs 전용. 파일 전체를 스크립트로 실행합니다.
-- 첫 실행에서는 아래 FALSE를 유지합니다. 삭제 후보와 연결을 막는 참조를 확인합니다.
-- 실제 교체는 검토한 뒤 TRUE로 바꿉니다. 삭제 대상 장비의 알람, 수신 이력,
-- 상태 및 수신 매핑도 함께 정리합니다. tb_log 참조가 있으면 교체를 중단합니다.
-- 실제 IP가 없는 신규 장비에는 미설정 값 0.0.0.0을 기록합니다.
BEGIN;
SET LOCAL lock_timeout = '5s';

CREATE TEMP TABLE device_rebuild_options ON COMMIT DROP AS
SELECT FALSE AS apply_changes, 'KA 수신 시험장비'::text AS keep_device_name,
       '2026-device-rebuild-ip-placeholder'::text AS script_version;

SELECT script_version, apply_changes FROM device_rebuild_options;

DO $check$
BEGIN
    IF current_database() <> 'c26hdcs' THEN
        RAISE EXCEPTION 'Run this script in c26hdcs. Current database: %', current_database();
    END IF;
    IF (SELECT apply_changes FROM device_rebuild_options) THEN
        LOCK TABLE public.tb_device, public.tb_observation_station, public.tb_device_group
            IN SHARE ROW EXCLUSIVE MODE;
    END IF;
END
$check$;

CREATE TEMP TABLE device_rebuild_source (
    obs_code text PRIMARY KEY,
    kind text NOT NULL,
    group_name text NOT NULL
) ON COMMIT DROP;

INSERT INTO device_rebuild_source (obs_code, kind, group_name) VALUES
    ('33014010', '강수량', '만경강'),
    ('33014020', '강수량', '만경강'),
    ('33014070', '강수량', '만경강'),
    ('33014080', '강수량', '만경강'),
    ('33014090', '강수량', '만경강'),
    ('33014100', '강수량', '만경강'),
    ('33014110', '강수량', '만경강'),
    ('33014120', '강수량', '만경강'),
    ('33024020', '강수량', '동진강'),
    ('33024030', '강수량', '동진강'),
    ('33024040', '강수량', '동진강'),
    ('33024050', '강수량', '동진강'),
    ('33024070', '강수량', '동진강'),
    ('33024080', '강수량', '동진강'),
    ('40024030', '강수량', '섬진강'),
    ('40024050', '강수량', '섬진강'),
    ('40024060', '강수량', '섬진강'),
    ('40034010', '강수량', '섬진강'),
    ('40034020', '강수량', '섬진강'),
    ('40034030', '강수량', '섬진강'),
    ('40044010', '강수량', '섬진강'),
    ('40044030', '강수량', '섬진강'),
    ('40044040', '강수량', '섬진강'),
    ('40044060', '강수량', '섬진강'),
    ('40044070', '강수량', '섬진강'),
    ('40054020', '강수량', '섬진강'),
    ('40054030', '강수량', '섬진강'),
    ('40054040', '강수량', '섬진강'),
    ('40064010', '강수량', '섬진강'),
    ('40084010', '강수량', '섬진강'),
    ('40084020', '강수량', '섬진강'),
    ('40094050', '강수량', '섬진강'),
    ('40094060', '강수량', '섬진강'),
    ('40094070', '강수량', '섬진강'),
    ('40094080', '강수량', '섬진강'),
    ('40094090', '강수량', '섬진강'),
    ('40094100', '강수량', '섬진강'),
    ('40094110', '강수량', '섬진강'),
    ('40094120', '강수량', '섬진강'),
    ('40094140', '강수량', '섬진강'),
    ('40094150', '강수량', '섬진강'),
    ('40094160', '강수량', '섬진강'),
    ('40094170', '강수량', '섬진강'),
    ('41044040', '강수량', '섬진강남해'),
    ('50014020', '강수량', '영산강'),
    ('50014030', '강수량', '영산강'),
    ('50014040', '강수량', '영산강'),
    ('50014050', '강수량', '영산강'),
    ('50014060', '강수량', '영산강'),
    ('50024020', '강수량', '영산강'),
    ('50024030', '강수량', '영산강'),
    ('50024040', '강수량', '영산강'),
    ('50034030', '강수량', '영산강'),
    ('50034040', '강수량', '영산강'),
    ('50034050', '강수량', '영산강'),
    ('50034060', '강수량', '영산강'),
    ('50034070', '강수량', '영산강'),
    ('50044030', '강수량', '영산강'),
    ('50044040', '강수량', '영산강'),
    ('50054010', '강수량', '영산강'),
    ('50054020', '강수량', '영산강'),
    ('50064020', '강수량', '영산강'),
    ('50074010', '강수량', '영산강'),
    ('50074020', '강수량', '영산강'),
    ('50084010', '강수량', '영산강'),
    ('51014020', '강수량', '탐진강'),
    ('51014030', '강수량', '탐진강'),
    ('51014080', '강수량', '탐진강'),
    ('51014090', '강수량', '탐진강'),
    ('51014100', '강수량', '탐진강'),
    ('3203690', '수위', '금강서해'),
    ('3301612', '수위', '만경강'),
    ('3301619', '수위', '만경강'),
    ('3301623', '수위', '만경강'),
    ('3301625', '수위', '만경강'),
    ('3301630', '수위', '만경강'),
    ('3301635', '수위', '만경강'),
    ('3301637', '수위', '만경강'),
    ('3301640', '수위', '만경강'),
    ('3301645', '수위', '만경강'),
    ('3301647', '수위', '만경강'),
    ('3301648', '수위', '만경강'),
    ('3301652', '수위', '만경강'),
    ('3301654', '수위', '만경강'),
    ('3301655', '수위', '만경강'),
    ('3301656', '수위', '만경강'),
    ('3301657', '수위', '만경강'),
    ('3301662', '수위', '만경강'),
    ('3301665', '수위', '만경강'),
    ('3301670', '수위', '만경강'),
    ('3301673', '수위', '만경강'),
    ('3301675', '수위', '만경강'),
    ('3301680', '수위', '만경강'),
    ('3301684', '수위', '만경강'),
    ('3301685', '수위', '만경강'),
    ('3301688', '수위', '만경강'),
    ('3301690', '수위', '만경강'),
    ('3302615', '수위', '동진강'),
    ('3302620', '수위', '동진강'),
    ('3302626', '수위', '동진강'),
    ('3302630', '수위', '동진강'),
    ('3302643', '수위', '동진강'),
    ('3302645', '수위', '동진강'),
    ('3302652', '수위', '동진강'),
    ('3302653', '수위', '동진강'),
    ('3302654', '수위', '동진강'),
    ('3302655', '수위', '동진강'),
    ('3302656', '수위', '동진강'),
    ('3302657', '수위', '동진강'),
    ('3302658', '수위', '동진강'),
    ('3302659', '수위', '동진강'),
    ('3302660', '수위', '동진강'),
    ('3302662', '수위', '동진강'),
    ('3302665', '수위', '동진강'),
    ('3302675', '수위', '동진강'),
    ('3302680', '수위', '동진강'),
    ('3302685', '수위', '동진강'),
    ('4001605', '수위', '섬진강'),
    ('4001610', '수위', '섬진강'),
    ('4001613', '수위', '섬진강'),
    ('4001617', '수위', '섬진강'),
    ('4001655', '수위', '섬진강'),
    ('4002613', '수위', '섬진강'),
    ('4002620', '수위', '섬진강'),
    ('4002640', '수위', '섬진강'),
    ('4002660', '수위', '섬진강'),
    ('4002690', '수위', '섬진강'),
    ('4003630', '수위', '섬진강'),
    ('4003650', '수위', '섬진강'),
    ('4003690', '수위', '섬진강'),
    ('4004615', '수위', '섬진강'),
    ('4004630', '수위', '섬진강'),
    ('4004632', '수위', '섬진강'),
    ('4004640', '수위', '섬진강'),
    ('4004650', '수위', '섬진강'),
    ('4004660', '수위', '섬진강'),
    ('4004680', '수위', '섬진강'),
    ('4004690', '수위', '섬진강'),
    ('4005627', '수위', '섬진강'),
    ('4005633', '수위', '섬진강'),
    ('4005645', '수위', '섬진강'),
    ('4005660', '수위', '섬진강'),
    ('4005670', '수위', '섬진강'),
    ('4005675', '수위', '섬진강'),
    ('4005690', '수위', '섬진강'),
    ('4006660', '수위', '섬진강'),
    ('4006667', '수위', '섬진강'),
    ('4006680', '수위', '섬진강'),
    ('4007610', '수위', '섬진강'),
    ('4007616', '수위', '섬진강'),
    ('4007661', '수위', '섬진강'),
    ('4008660', '수위', '섬진강'),
    ('4008670', '수위', '섬진강'),
    ('4009601', '수위', '섬진강'),
    ('4009608', '수위', '섬진강'),
    ('4009610', '수위', '섬진강'),
    ('4009618', '수위', '섬진강'),
    ('4009622', '수위', '섬진강'),
    ('4009625', '수위', '섬진강'),
    ('4009629', '수위', '섬진강'),
    ('4009630', '수위', '섬진강'),
    ('4009635', '수위', '섬진강'),
    ('4009638', '수위', '섬진강'),
    ('4009640', '수위', '섬진강'),
    ('4009650', '수위', '섬진강'),
    ('4009665', '수위', '섬진강'),
    ('4009667', '수위', '섬진강'),
    ('4009669', '수위', '섬진강'),
    ('4009670', '수위', '섬진강'),
    ('4009690', '수위', '섬진강'),
    ('4101620', '수위', '섬진강남해'),
    ('4101640', '수위', '섬진강남해'),
    ('4102650', '수위', '섬진강남해'),
    ('4104640', '수위', '섬진강남해'),
    ('4104665', '수위', '섬진강남해'),
    ('4104675', '수위', '섬진강남해'),
    ('4104680', '수위', '섬진강남해'),
    ('4104685', '수위', '섬진강남해'),
    ('4104690', '수위', '섬진강남해'),
    ('4105625', '수위', '섬진강남해'),
    ('4105630', '수위', '섬진강남해'),
    ('4105633', '수위', '섬진강남해'),
    ('4105640', '수위', '섬진강남해'),
    ('4105694', '수위', '섬진강남해'),
    ('5001610', '수위', '영산강'),
    ('5001615', '수위', '영산강'),
    ('5001620', '수위', '영산강'),
    ('5001623', '수위', '영산강'),
    ('5001625', '수위', '영산강'),
    ('5001627', '수위', '영산강'),
    ('5001628', '수위', '영산강'),
    ('5001630', '수위', '영산강'),
    ('5001632', '수위', '영산강'),
    ('5001640', '수위', '영산강'),
    ('5001645', '수위', '영산강'),
    ('5001650', '수위', '영산강'),
    ('5001655', '수위', '영산강'),
    ('5001660', '수위', '영산강'),
    ('5001669', '수위', '영산강'),
    ('5001670', '수위', '영산강'),
    ('5001673', '수위', '영산강'),
    ('5001676', '수위', '영산강'),
    ('5001680', '수위', '영산강'),
    ('5001682', '수위', '영산강'),
    ('5002610', '수위', '영산강'),
    ('5002620', '수위', '영산강'),
    ('5002630', '수위', '영산강'),
    ('5002643', '수위', '영산강'),
    ('5002650', '수위', '영산강'),
    ('5002660', '수위', '영산강'),
    ('5002665', '수위', '영산강'),
    ('5002666', '수위', '영산강'),
    ('5002677', '수위', '영산강'),
    ('5002690', '수위', '영산강'),
    ('5003602', '수위', '영산강'),
    ('5003605', '수위', '영산강'),
    ('5003610', '수위', '영산강'),
    ('5003612', '수위', '영산강'),
    ('5003613', '수위', '영산강'),
    ('5003615', '수위', '영산강'),
    ('5003620', '수위', '영산강'),
    ('5003640', '수위', '영산강'),
    ('5003650', '수위', '영산강'),
    ('5003680', '수위', '영산강'),
    ('5004620', '수위', '영산강'),
    ('5004625', '수위', '영산강'),
    ('5004630', '수위', '영산강'),
    ('5004645', '수위', '영산강'),
    ('5004650', '수위', '영산강'),
    ('5004655', '수위', '영산강'),
    ('5004670', '수위', '영산강'),
    ('5004684', '수위', '영산강'),
    ('5004690', '수위', '영산강'),
    ('5004696', '수위', '영산강'),
    ('5004698', '수위', '영산강'),
    ('5005630', '수위', '영산강'),
    ('5005650', '수위', '영산강'),
    ('5005680', '수위', '영산강'),
    ('5006610', '수위', '영산강'),
    ('5006617', '수위', '영산강'),
    ('5006620', '수위', '영산강'),
    ('5006630', '수위', '영산강'),
    ('5006670', '수위', '영산강'),
    ('5006673', '수위', '영산강'),
    ('5007630', '수위', '영산강'),
    ('5007640', '수위', '영산강'),
    ('5007650', '수위', '영산강'),
    ('5008670', '수위', '영산강'),
    ('5008690', '수위', '영산강'),
    ('5008695', '수위', '영산강'),
    ('5101650', '수위', '탐진강'),
    ('5101660', '수위', '탐진강'),
    ('5101670', '수위', '탐진강'),
    ('5101675', '수위', '탐진강'),
    ('5101690', '수위', '탐진강'),
    ('5201640', '수위', '영산강남해'),
    ('5202670', '수위', '영산강남해'),
    ('5301635', '수위', '영산강서해'),
    ('5301660', '수위', '영산강서해'),
    ('5301690', '수위', '영산강서해'),
    ('5302620', '수위', '영산강서해'),
    ('5302642', '수위', '영산강서해'),
    ('5302660', '수위', '영산강서해'),
    ('6001650', '수위', '제주도'),
    ('6002670', '수위', '제주도'),
    ('6003620', '수위', '제주도'),
    ('6004670', '수위', '제주도');

CREATE TEMP TABLE device_rebuild_plan ON COMMIT DROP AS
SELECT i.obs_code, i.kind, i.group_name, s.station_id, g.group_id, g.use_yn,
       s.name || ' [' || i.obs_code || ']' AS device_name,
       '0.0.0.0'::text AS planned_ip_address
FROM device_rebuild_source i
LEFT JOIN public.tb_observation_station s ON btrim(s.obs_code) = i.obs_code
LEFT JOIN public.tb_device_group g ON btrim(g.group_name) = i.group_name;

CREATE TEMP TABLE device_rebuild_old ON COMMIT DROP AS
SELECT d.device_id, d.device_name, d.device_type, d.station_id,
       lower(regexp_replace(d.device_name, '[[:space:]]+', '', 'g'))
           IS NOT DISTINCT FROM lower(regexp_replace(o.keep_device_name, '[[:space:]]+', '', 'g'))
           AS keep_device
FROM public.tb_device d CROSS JOIN device_rebuild_options o;

SELECT device_id, device_name, device_type, station_id,
       CASE
           WHEN (SELECT count(*) FROM device_rebuild_old WHERE keep_device) <> 1 THEN 'BLOCKED'
           WHEN keep_device THEN 'KEEP'
           ELSE 'DELETE'
       END AS action
FROM device_rebuild_old ORDER BY keep_device DESC, device_id;

SELECT obs_code, device_name, kind, group_name, station_id, group_id, use_yn, planned_ip_address
FROM device_rebuild_plan ORDER BY kind, obs_code;

CREATE TEMP TABLE device_rebuild_references (
    referencing_table text,
    referenced_table text,
    constraint_name text,
    referenced_rows bigint,
    action text
) ON COMMIT DROP;

CREATE TEMP TABLE device_rebuild_delete_rows (
    relation_id oid,
    row_id tid,
    depth integer NOT NULL,
    PRIMARY KEY (relation_id, row_id)
) ON COMMIT DROP;

INSERT INTO device_rebuild_delete_rows
SELECT 'public.tb_device'::regclass, d.ctid, 0
FROM ONLY public.tb_device d
JOIN device_rebuild_old old USING (device_id)
WHERE NOT old.keep_device;

-- 허용된 상세 기록까지 외래 키로 찾아 삭제 계획에 추가합니다.
-- 다른 참조는 건수를 표시하고 실제 교체를 중단합니다.
DO $references$
DECLARE
    fk record;
    parent_depth integer;
    join_condition text;
    row_query text;
    reference_count bigint;
    can_delete boolean;
BEGIN
  FOR parent_depth IN 0..4 LOOP
    FOR fk IN
        SELECT c.oid, c.conname, c.conrelid, c.confrelid, c.conkey, c.confkey
        FROM pg_constraint c
        WHERE c.contype = 'f'
          AND c.confrelid IN (
              SELECT relation_id FROM device_rebuild_delete_rows WHERE depth = parent_depth
          )
          AND c.conparentid = 0
        ORDER BY c.conrelid, c.oid
    LOOP
        IF (SELECT apply_changes FROM device_rebuild_options) THEN
            EXECUTE format('LOCK TABLE %s IN SHARE ROW EXCLUSIVE MODE', fk.conrelid::regclass);
        END IF;

        SELECT string_agg(format('r.%I = d.%I', child.attname, parent.attname), ' AND ')
        INTO join_condition
        FROM unnest(fk.conkey, fk.confkey) AS keys(child_num, parent_num)
        JOIN pg_attribute child ON child.attrelid = fk.conrelid AND child.attnum = keys.child_num
        JOIN pg_attribute parent ON parent.attrelid = fk.confrelid AND parent.attnum = keys.parent_num;

        row_query := format(
            'FROM %s r JOIN %s d ON %s
             JOIN device_rebuild_delete_rows p ON p.relation_id = d.tableoid
              AND p.row_id = d.ctid AND p.depth = %s',
            fk.conrelid::regclass, fk.confrelid::regclass, join_condition, parent_depth
        );
        EXECUTE 'SELECT count(*) ' || row_query INTO reference_count;
        IF reference_count = 0 THEN
            CONTINUE;
        END IF;

        can_delete := CASE
            WHEN parent_depth = 0 THEN fk.conrelid IN (
                to_regclass('public.tb_alarm'), to_regclass('public.tb_dcs_data'),
                to_regclass('public.tb_ka_data'), to_regclass('public.tb_device_status'),
                to_regclass('public.tb_python_alarm_state'), to_regclass('public.tb_python_component_status'),
                to_regclass('public.tb_python_device_mapping')
            )
            WHEN parent_depth = 1 THEN
                (fk.confrelid = to_regclass('public.tb_alarm') AND fk.conrelid = to_regclass('public.tb_alarm_check'))
                OR (fk.confrelid = to_regclass('public.tb_dcs_data') AND fk.conrelid = to_regclass('public.tb_dcs_receive_details'))
                OR (fk.confrelid = to_regclass('public.tb_dcs_data') AND fk.conrelid = to_regclass('public.tb_dcs_delivery_jobs'))
                OR (fk.confrelid = to_regclass('public.tb_ka_data') AND fk.conrelid = to_regclass('public.tb_ka_receive_details'))
            WHEN parent_depth = 2 THEN
                (fk.confrelid = to_regclass('public.tb_dcs_receive_details')
                 AND fk.conrelid = to_regclass('public.tb_dcs_delivery_jobs'))
                OR (fk.confrelid = to_regclass('public.tb_dcs_delivery_jobs')
                    AND fk.conrelid = to_regclass('public.tb_dcs_delivery_attempts'))
            WHEN parent_depth = 3 THEN
                fk.confrelid = to_regclass('public.tb_dcs_delivery_jobs')
                AND fk.conrelid = to_regclass('public.tb_dcs_delivery_attempts')
            ELSE FALSE
        END;

        -- 행 주소는 일반 테이블에서만 사용합니다.
        can_delete := COALESCE(can_delete, FALSE) AND EXISTS (
            SELECT FROM pg_class WHERE oid = fk.conrelid AND relkind = 'r' AND NOT relispartition
        ) AND NOT EXISTS (SELECT FROM pg_inherits WHERE inhparent = fk.conrelid);

        INSERT INTO device_rebuild_references
        VALUES (fk.conrelid::regclass::text, fk.confrelid::regclass::text, fk.conname, reference_count,
                CASE WHEN can_delete THEN 'DELETE' ELSE 'BLOCKED' END);

        IF can_delete THEN
            EXECUTE format(
                'INSERT INTO device_rebuild_delete_rows
                 SELECT r.tableoid, r.ctid, %s %s
                 ON CONFLICT (relation_id, row_id) DO UPDATE
                 SET depth = GREATEST(device_rebuild_delete_rows.depth, EXCLUDED.depth)',
                parent_depth + 1, row_query
            );
        END IF;
    END LOOP;
  END LOOP;
END
$references$;

SELECT * FROM device_rebuild_references ORDER BY referencing_table, constraint_name;

DO $apply$
DECLARE
    target record;
    removed_rows bigint;
    deleted_devices integer;
    added_devices integer;
BEGIN
    IF NOT (SELECT apply_changes FROM device_rebuild_options) THEN
        RAISE NOTICE 'Preview only. Check KEEP, DELETE, the 267 planned devices, and foreign key references.';
        RETURN;
    END IF;

    IF (SELECT count(*) FROM device_rebuild_old WHERE keep_device) <> 1 THEN
        RAISE EXCEPTION 'Exactly one Ka test device must match keep_device_name.';
    END IF;
    IF (SELECT count(*) FROM device_rebuild_delete_rows WHERE depth = 0)
           <> (SELECT count(*) FROM device_rebuild_old WHERE NOT keep_device)
       OR EXISTS (SELECT FROM pg_inherits WHERE inhparent = 'public.tb_device'::regclass) THEN
        RAISE EXCEPTION 'The device table layout does not support this cleanup.';
    END IF;
    IF (SELECT count(*) FROM device_rebuild_plan) <> 267 OR EXISTS (
        SELECT FROM device_rebuild_plan
        WHERE station_id IS NULL OR device_name IS NULL OR group_id IS NULL OR use_yn IS DISTINCT FROM 'Y'
    ) THEN
        RAISE EXCEPTION 'Expected 267 unique observation stations with active groups.';
    END IF;
    IF EXISTS (
        SELECT FROM device_rebuild_old d JOIN device_rebuild_plan p ON p.station_id = d.station_id
        WHERE d.keep_device
    ) THEN
        RAISE EXCEPTION 'The Ka test device already occupies a station in the import list.';
    END IF;
    IF EXISTS (
        SELECT FROM device_rebuild_old d JOIN device_rebuild_plan p ON p.device_name = d.device_name
    ) THEN
        RAISE EXCEPTION 'A planned device name already exists. Check for a previous rebuild before proceeding.';
    END IF;
    IF EXISTS (SELECT FROM device_rebuild_references WHERE action = 'BLOCKED') THEN
        RAISE EXCEPTION 'Review BLOCKED references before deleting devices.';
    END IF;

    FOR target IN
        SELECT relation_id, max(depth) AS depth, count(*) AS expected_rows
        FROM device_rebuild_delete_rows
        WHERE depth > 0
        GROUP BY relation_id
        ORDER BY depth DESC,
                 (relation_id = to_regclass('public.tb_python_device_mapping')) ASC,
                 relation_id
    LOOP
        EXECUTE format(
            'DELETE FROM ONLY %s r USING device_rebuild_delete_rows p
             WHERE p.relation_id = r.tableoid AND p.row_id = r.ctid',
            target.relation_id::regclass
        );
        GET DIAGNOSTICS removed_rows = ROW_COUNT;
        IF removed_rows <> target.expected_rows THEN
            RAISE EXCEPTION 'Cleanup count changed for %.', target.relation_id::regclass;
        END IF;
    END LOOP;

    DELETE FROM ONLY public.tb_device d
    USING device_rebuild_old old
    WHERE d.device_id = old.device_id AND NOT old.keep_device;
    GET DIAGNOSTICS deleted_devices = ROW_COUNT;

    INSERT INTO public.tb_device (device_name, device_type, ip_address, device_status, station_id, group_id)
    SELECT device_name, kind, '0.0.0.0', 'WARNING', station_id, group_id
    FROM device_rebuild_plan ORDER BY obs_code;
    GET DIAGNOSTICS added_devices = ROW_COUNT;

    IF added_devices <> 267 OR EXISTS (
        SELECT FROM device_rebuild_plan p LEFT JOIN public.tb_device d ON d.station_id = p.station_id
        GROUP BY p.station_id, p.group_id
        HAVING count(d.device_id) <> 1 OR count(d.device_id) FILTER (WHERE d.group_id = p.group_id) <> 1
    ) THEN
        RAISE EXCEPTION 'Station to device validation failed.';
    END IF;

    RAISE NOTICE 'Deleted %, created %, preserved 1 Ka test device.', deleted_devices, added_devices;
END
$apply$;
COMMIT;

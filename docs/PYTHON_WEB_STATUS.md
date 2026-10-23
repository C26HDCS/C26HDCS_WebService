# Python 상태와 웹 표시 연동

## 적용 순서

1. Python 수신·감시·전송 프로세스를 정상 종료합니다.
2. 기존 운영 PostgreSQL에서 `src/main/resources/sql/python_device_status_postgresql.sql` 전체를 실행합니다. 기존 파이프라인·로그/알람 테이블이 준비된 DB에 적용합니다. 이 스크립트는 기존 수신 자료를 삭제하지 않습니다. 초기화용 `schema.sql`은 실행하지 않습니다.
3. WebService를 빌드하여 재시작합니다. Python도 변경된 코드로 재시작합니다. Python 코드는 `D:/Project/C26HDCS_Python`에 반영했습니다.
4. Python 실행 환경은 기존 PostgreSQL 설정을 유지합니다. `python -m hdcs_db check`의 `schema_ready`가 `true`인지 확인합니다.
5. DCS 관측소별 감시와 Ka 카메라별 감시, 수신기 및 전송기를 지속 실행합니다. `--once`는 일회 시험용이며, 실행 후 종료하면 주기 갱신이 중단된 것으로 판정합니다.

```powershell
cd D:\Project\C26HDCS_Python
$env:HDCS_DB_BACKEND = 'postgresql'
$env:HDCS_STATUS_TTL_SECONDS = '120'
& .\.venv\Scripts\python.exe -m hdcs_db check

# 예시: 기존 매핑의 관측소 코드/카메라 ID로 실행
& .\.venv\Scripts\python.exe -m dcs_receiver monitor --station-key '44/56/01'
# Ka 감시는 다른 창에서 실행
& .\.venv\Scripts\python.exe -m ka_receiver.monitor --camera-id cam_test
```

위 두 명령은 감시기입니다. 수신기와 전송기도 기존 실행 안내에 따라 별도 창/서비스로 실행해야 합니다. 한 장비의 자료로 다른 관측소를 정상으로 표시하지 않도록 DCS는 관측소별 `--station-key` 감시를 권장합니다. 전체 감시는 명확히 하나의 매핑으로 귀속될 때만 관측소 상태를 갱신합니다.

수신 간격 기준(`--stale-after-seconds`), Ka 수신기 heartbeat 기준, dump 대기 기준은 기존 감시 옵션을 그대로 사용합니다. `HDCS_STATUS_TTL_SECONDS`는 **모듈 상태 갱신의 유효 시간**으로 자료 수신 주기와 다릅니다. 기본 120초이며 최소 10초입니다. 긴 업로드/배치 처리나 감시 간격이 이 시간을 넘지 않도록 타임아웃·배치 크기·TTL을 함께 설정합니다. 단일 처리가 TTL보다 오래 걸리면 상태 갱신 지연으로 장애가 표시될 수 있습니다.

## DB 구조

| 저장 위치 | 역할 |
| --- | --- |
| `tb_python_component_status` | 장비·대역·모듈·범위별 최신 상태와 갱신/만료 시각 |
| `v_python_component_health` | 만료된 상태를 ERROR로 판정하는 조회 뷰 |
| `tb_device` | Python 관리 장비의 대표 상태 및 실제 최근 수신 시각/결과 |
| `tb_device_status` | 대표 상태가 변경된 경우에만 이력 저장 |
| `tb_alarm`, `tb_log` | 갱신 시간 초과 알람과 장애/복구 로그 |

대표 상태는 **ERROR → WARNING → NORMAL** 우선순위로 합산합니다. 수신이 정상이더라도 미해결 전송 재시도가 남아 있으면 정상으로 덮어쓰지 않습니다. 전송은 작업별 상태를 유지해 다른 작업의 성공이 이전 실패를 지우지 않게 했습니다. 완료된 전송 작업은 기존 전송 이력을 유지하고 실시간 상태 행만 정리해 상태 조회량이 무한히 늘지 않게 합니다. 기존 전송기는 무제한 재시도 방식이므로 재시도 상태는 WARNING이며 별도의 최대 재시도 횟수는 이번 변경에 추가하지 않았습니다.

자료 저장 시 `last_received_at`, `last_receive_status`를 갱신합니다. 오래된 자료가 늦게 저장되어도 최근 수신 시각이 과거로 되돌아가지 않습니다. 감시·전송 heartbeat는 수신 시각을 변경하지 않습니다. 장비를 식별할 수 없는 프레임은 임의 장비에 수신 시각을 기록하지 않습니다.

새 매핑을 등록하면 첫 감시를 기다리는 MONITOR 상태도 등록합니다. 초기 120초 동안 WARNING이고, 이후 감시가 없으면 ERROR로 바뀝니다. 기존 매핑도 이번 SQL 적용 시 동일하게 초기화합니다(이미 있는 상태는 덮어쓰지 않음).

상태 소유권은 매핑된 Python 자료 처리 장비에 있습니다. 같은 `device_id`에 NMS 등 별도 프로그램이 `device_status`를 직접 덮어쓰면 충돌하므로 독립 장비 매핑 또는 단일 상태 집계 경로를 사용하십시오. 장비 매핑/전송 목적지를 바꾸거나 모듈을 운영에서 제거할 때는 이전 범위의 상태 행도 검토해야 합니다. 기존 범위는 자동으로 정상 처리하거나 지우지 않습니다.

## 웹 동작

- `GET /api/dashboard/live`: 시간 초과 반영 후 장비 목록·집계·모듈 상세를 함께 반환합니다.
- 대시보드는 5초마다 조회합니다. 중복 요청은 방지하며 화면을 떠나면 타이머를 정리합니다.
- 오류 시 마지막 정상 조회 값을 보존하고 **상태 갱신 실패**를 표시합니다. 처음부터 조회되지 않으면 수치를 `-`로 표시하고 임의 장비 수치를 보여주지 않습니다.
- 시스템 구성도의 L/Ka 박스 테두리는 각 대역의 Python 자료 처리 상태를 표시합니다. 정보가 없으면 회색입니다. 방화벽·IPS 등의 하드웨어 상태를 추정해 표시하지 않습니다.
- 장비 상세에 모듈별 상태, 범위, 상세 사유, 점검 시각을 표시합니다.
- WebService의 주기 점검(기본 5초)은 대시보드를 열지 않아도 갱신 만료를 검사합니다. 시간 초과 알람은 장비별 한 장애 구간에 한 번만 생성하며 복구 후 재발하면 다시 생성합니다. 기존 알람의 사용자 읽음 상태는 바꾸지 않습니다.
- Python과 WebService가 모두 종료되거나 DB 자체가 중단된 상황은 이 내부 감시만으로 감지·저장할 수 없습니다. 외부 프로세스/DB 가용성 감시가 필요합니다.

## 확인 시험

1. 기존 장비 매핑을 확인하고 각 수신기·감시기·전송기를 실행합니다.
2. DCS 샘플/Ka 영상+dump를 수신하여 `tb_device.last_received_at`과 수신 결과, 웹 장비 목록 갱신을 확인합니다.
3. 감시기는 유지하고 자료 입력을 중단하여 자료 지연 WARNING을 확인합니다.
4. 감시기를 종료한 뒤 TTL+웹 점검 간격 후 ERROR와 `STATUS_TIMEOUT` 알람을 확인합니다. 반복 조회해도 같은 장애 알람이 계속 생기지 않아야 합니다.
5. 감시기를 재실행하여 다른 장애가 없을 때 정상 복구와 이력을 확인합니다.
6. 전송 실패를 만든 후 수신 정상화만으로 장비가 NORMAL이 되지 않는지 확인합니다. 실패한 작업을 재전송하여 정상 복구를 확인합니다.
7. DB 연결 실패 시 임의 정상 수치가 표시되지 않는지 확인합니다.

```sql
SELECT device_id, data_kind, component, scope, effective_status, detail, checked_at, expires_at, expired
FROM public.v_python_component_health ORDER BY device_id, component, scope;

SELECT device_id, device_status, last_received_at, last_receive_status
FROM public.tb_device WHERE device_id IN (75, 86);

SELECT * FROM public.tb_device_status ORDER BY checked_at DESC LIMIT 30;
```

테스트는 운영 DB 변경 없이 로컬 임시 PostgreSQL, SQLite, Java/JavaScript 테스트로 수행합니다. 운영 DB에는 SQL을 자동 적용하지 않았습니다.

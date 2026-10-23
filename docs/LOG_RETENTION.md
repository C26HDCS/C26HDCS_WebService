# 로그인 이벤트 및 로그 보관 정책

## 적용 순서

1. 기존 DB를 백업한 후 운영 DB에 `src/main/resources/sql/log_audit_retention_postgresql.sql`을 적용한다.
   `schema.sql`은 초기화용이므로 운영 DB에서 실행하지 않는다. 기존 기록은 삭제하지 않는다.
2. WebService를 다시 빌드·시작하고, Python 수신·감시·전송 프로세스도 재시작한다.
3. `tb_event`의 `LOGIN_SUCCESS`, `LOGIN_FAIL`, `LOGOUT` 및 파일의 `HEARTBEAT`를 확인한다.
   DB 스키마 적용 전에는 이벤트가 파일 대기열에 남고 DB 조회에는 나타나지 않을 수 있다.

## 인증 이벤트

- 로그인 성공·실패 및 인증된 `POST /api/logout` 요청을 이벤트로 저장한다.
- `user_id`는 현재 등록된 사용자 연결이며, `actor_user_id`는 당시 입력/인증된 ID의 보존 사본이다.
  미등록 ID의 실패도 저장하고, 사용자 삭제 시 연결만 NULL로 바뀌며 과거 ID와 기록은 남는다.
- 비밀번호·JWT·인증 예외 상세는 이벤트에 저장하지 않는다. 실패 시 입력 ID는 인증된 사용자라는 뜻이 아니다.
- UUID `record_key`가 중복 저장을 막는다. 이벤트 시각은 한국 시각의 초 단위이다.
- 이벤트는 `logs/audit-pending/<UUID>.json`에 강제 기록한 후 DB 저장을 시도한다.
  실패하면 1초 주기의 별도 작업에서 재시도하며, 재시작 후에도 이어 처리한다.
  파일 이름 변경 전에 중단되어 남은 완전한 `.tmp` 항목도 60초 후 다시 읽어 처리한다.
  DB 커밋 후에만 대기 파일을 삭제한다. 손상 파일은 `.invalid`로 보존하고 오류를 기록한다.
- 디스크에도 기록할 수 없으면 로그인/로그아웃 완료로 응답하지 않고 503을 반환한다.
  DB 장애 중 로컬 대기열에 보존된 상태와 DB 저장 완료는 구분한다.
- 로그아웃 버튼은 서버 응답 후 브라우저 토큰을 제거한다. 창 강제 종료는 명시적 로그아웃 요청이 아니다.
  기존 JWT의 서버 측 폐기/블랙리스트 기능은 이번 변경 범위에 포함하지 않는다.

## 1초 생성 및 저장

- WebService의 전용 하트비트 스레드는 업무 요청이 없어도 1초마다 파일 로그를 생성하고
  활성 파일에 `FileChannel.force(true)`를 수행한다. DB 재시도 스레드와 분리하여 DB 지연의 영향을 줄였다.
- Python은 프로세스별로 1초마다 하트비트를 생성하고 파일을 flush/fsync한다.
  업무 로그도 즉시 flush하고, 같은 파일의 주기적 fsync에 포함한다.
  DCS/Ka 모듈 실행 후 첫 로그 또는 감시/전송 주기 진입 시 하트비트가 시작된다.
- 하트비트는 파일에 저장한다. DB에 1초마다 불필요한 행을 추가하지 않는다.
  수신·감시 변경·전송·인증 이벤트의 DB 저장은 각각의 업무 발생 시 수행한다.
- 실제 지연이 1.5초를 넘으면 `LOG_INTERVAL_GAP`, 기록 실패는 `LOG_PERSISTENCE_FAILURE`로 알린다.
  누락된 과거 초에 정상 동작한 것처럼 기록을 만들어 채우지 않는다.
- 일반 OS 스케줄링, 프로세스 종료, 디스크 장애·전원 차단 상황에서도 정확히 1초 이내 저장되는
  하드 실시간 보장은 아니다. 타이밍·누락 시험은 정상/최대 부하에서 실측해야 한다.

## 최소 5년 보관

- DB의 `tb_log`, `tb_event`는 최근 5년 기록의 수정·삭제와 전체 비우기를 트리거로 차단한다.
  사용자 삭제 시의 FK NULL 처리만 예외로 허용한다. 일자 경계는 한국 시각이고 경계 날짜 전체를 보존한다.
- DB의 5년 초과 기록 자동 정리는 기본 OFF이다. 명시적으로 `HDCS_LOG_PURGE_EXPIRED=true`를
  설정하면 하루 한 번 최대 1000행씩 각 테이블에서 정리한다. 대량 삭제나 백업 삭제는 하지 않는다.
- WebService는 일자/50MB 단위로 gzip 압축하며 1828일분의 과거 파일을 보관한다.
  윤년을 포함한 5년보다 짧아지지 않게 여유를 두었으며, 용량 상한에 따른 조기 삭제는 사용하지 않는다.
- Python은 PID와 시작 UUID를 포함한 일자별 파일을 사용한다. 날짜가 바뀌면 이전 파일을 gzip 압축하고
  해시 확인 후 원본을 정리한다. 달력 기준 5년 이전의 관리 대상 압축 파일만 정리한다.
  기존 PID 로그/회전 백업 및 비정상 종료로 남은 비압축 파일은 임의로 지우지 않는다.
- 웹 저장장치 화면의 수동 삭제도 WebService/Python 로그 경로의 5년 이내 파일을 건너뛴다.
  미전송 인증 이벤트 대기열은 파일 나이에 관계없이 수동 삭제를 차단한다.
- 보호 경로: `HDCS_WEB_LOG_DIR`(기본 `logs`), `HDCS_PYTHON_LOG_DIR`(기본 `../C26HDCS_Python/logs`).
  Python에서 별도 `HDCS_LOG_DIR`을 사용하면 웹의 `HDCS_PYTHON_LOG_DIR`도 같은 실제 경로로 지정한다.

## 운영 전제 및 검증

- 로그·대기열은 애플리케이션 계정만 쓸 수 있는 로컬 NTFS 등 원자적 파일 이동을 지원하는 디스크에 둔다.
  같은 디렉터리를 여러 WebService 인스턴스에서 동시에 사용하지 않는다.
  OS/DB 시각 동기화를 유지하고, 앱 계정의 테이블·시퀀스 권한과 디스크 쓰기 권한을 확인한다.
- DB 트리거는 별도 관리자에 의한 DROP/트리거 해제까지 막는 보안 장치는 아니다.
  OS 파일 직접 삭제·임의의 외부 삭제 프로그램 역시 앱에서 막을 수 없다.
- 여유 공간 1GB 미만이면 오류를 기록하며 5년 이내 로그를 자동 삭제하여 공간을 확보하지 않는다.
  필요한 용량은 실제 일일 증가량 × 5년 실제 일수 + 인덱스/백업/예비 용량으로 산정한다.
  압축률·보관 디스크·백업·복구 시험이 있어야 장비 고장까지 포함한 장기 보존을 확인할 수 있다.
- 과거의 30일/백업 5개 정책으로 이미 삭제된 기록은 이번 변경으로 복구되지 않는다.
- H2/단위 테스트: `mvn -Dtest=LogServiceTest,AuthAuditTest,LogPolicyTest,LogbackPolicyTest test`
- 브라우저 스크립트 테스트: `node --test src/test/js/*.test.cjs`
- Python: SQLite 모드에서 `python -m unittest discover -s tests -v`.
  PostgreSQL 보관 정책 시험은 `HDCS_TEST_RETENTION_POSTGRES=1`, `HDCS_RETENTION_SQL` 및
  별도의 localhost `hdcs_test_*` 데이터베이스에서만 실행한다.

```sql
SELECT id, COALESCE(user_id, actor_user_id) AS actor, event_type, message, reg_dt
FROM public.tb_event
WHERE event_type IN ('LOGIN_SUCCESS','LOGIN_FAIL','LOGOUT')
ORDER BY reg_dt DESC, id DESC LIMIT 30;
```

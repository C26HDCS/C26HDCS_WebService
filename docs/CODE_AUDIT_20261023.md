# WebService / Python 전체 점검 결과

점검일: 2026-10-23 (Asia/Seoul)

## 범위와 한계

- WebService의 매퍼 XML 전체와 주요 컨트롤러·서비스·환경설정, Python의 DB 연결·수신·감시·전송·상태 집계 경로를 검토했습니다.
- 직전 작업에서 고친 저장장치 매퍼 `keyColumn="id"` 변경은 유지했습니다. 다른 자동 키 반환 설정에서도 동일한 대문자 `keyColumn="ID"` 패턴은 발견하지 않았습니다.
- 운영 PostgreSQL의 구조를 읽기 전용으로 확인하려 했으나 연결 슬롯 소진(SQLSTATE 53300)으로 거절됐습니다. 운영 DB의 모든 컬럼·제약조건·시퀀스와 매퍼의 일치를 검증했다는 의미는 아닙니다.
- 운영 서버 데이터·세션·테이블은 변경하지 않았습니다. 이번 작업은 점검이며 아래 결함을 수정하지 않았습니다.

## 주요 발견 사항

### F01 / P1: DCS TCP 연결 수에 따라 DB 세션이 제한 없이 증가할 수 있음

근거: `D:/Project/C26HDCS_Python/dcs_receiver/__main__.py:67`, `D:/Project/C26HDCS_Python/dcs_receiver/storage.py:40`

TCP Handler마다 FrameStore를 만들며, FrameStore가 DB 연결을 엽니다. 클라이언트가 TCP 연결을 유지하면 `recv()` 대기 중에도 DB 연결을 유지합니다. ThreadingTCPServer의 동시 접속 상한과 accepted socket 유휴 타임아웃은 설정되어 있지 않습니다. 연결을 장기간 유지하는 관측소/중복 클라이언트가 늘면 DB 슬롯이 함께 증가합니다. 현재 장애의 실제 점유자는 pg_stat_activity로 별도 확인해야 하며, 이 코드만으로 현재 장애 원인을 단정하지 않습니다.

조치: 제한된 수신 큐와 저장 워커/DB 풀, 접속 상한, 유휴 타임아웃 및 정상 종료 시 클라이언트 연결 정리를 적용합니다. 웹·Python·관리 도구의 전체 연결 예산을 정해야 합니다.

### F02 / P1: 공개 회원가입에서 관리자 역할을 요청할 수 있음

근거: `src/main/java/kr/kwater/hdcs/common/web/PublicApiController.java:46`, `src/main/java/kr/kwater/hdcs/user/dao/UserDAO.java:48`

`/api/public/**`는 비로그인 접근 가능하며, 회원가입은 역할 값이 없을 때만 USER를 설정합니다. 전달된 역할을 DAO가 role_type에 저장하고 로그인 서비스가 이를 Spring Security 역할로 사용합니다. 따라서 공개 가입 요청에 ADMIN 역할을 지정하면 서버 코드상 그대로 저장됩니다. 실제 운영 계정을 만들거나 공격 요청을 보내지는 않았습니다.

조치: 공개 회원가입의 역할은 서버에서 USER로 고정하고, 관리자 부여는 관리자 전용 API로 제한합니다.

### F03 / P1: 일반 사용자가 관리자 작업 API를 호출할 수 있음

근거: `src/main/java/kr/kwater/hdcs/config/SecurityConfig.java:56`, `src/main/java/kr/kwater/hdcs/user/web/UserApiController.java:60`, `src/main/java/kr/kwater/hdcs/storage/web/StorageController.java:48`

로그인 여부만 검사하고 역할을 검사하지 않습니다. 일반 사용자가 사용자 수정·비밀번호 변경·삭제, 설정 변경, 서버 파일 삭제 API를 직접 호출할 수 있습니다. 화면 버튼 숨김만으로는 차단할 수 없습니다. 대시보드 전체 API도 permitAll이며 `/api/dashboard/live`는 조회 과정에서 상태 반영 함수까지 호출합니다.

조치: HTTP 메서드와 URL별 권한을 명시하고 관리자 API에 ADMIN 검사를 적용합니다. 일반 사용자의 허용 작업(조회/자기 정보/알람 확인 등)은 별도 정의합니다.

### F04 / P1: DB 자격증명과 JWT 서명키가 저장소 설정에 고정됨

근거: `src/main/resources/application.yml:19`, `src/main/resources/application.yml:104`

설정 파일에 접속 비밀번호와 JWT 서명키가 들어 있습니다. 소스·빌드 산출물을 읽을 수 있는 사람이 운영 자격증명과 토큰 서명키를 알 수 있으며, 같은 키를 쓰는 운영 환경의 인증 경계를 무너뜨릴 수 있습니다. 보고서에는 비밀 값 자체를 복사하지 않았습니다.

조치: 환경변수/비밀 저장소로 외부화하고 기존 운영 키·비밀번호를 교체합니다. 키 교체 시 기존 로그인 토큰이 무효화되는 영향도 공지해야 합니다.

### F05 / P2: DB 장애 시 고정 주기 재시도와 긴 로그가 반복됨

근거: `src/main/java/kr/kwater/hdcs/log/service/LogMaintenance.java:37`, `src/main/java/kr/kwater/hdcs/alarm/service/impl/AlarmStreamServiceImpl.java:59`, `src/main/java/kr/kwater/hdcs/dashboard/service/impl/PipelineStatusService.java:23`

감사 재저장과 접속 중 알람 감시는 1초, 상태 감시는 5초 고정 주기입니다. DB 연결이 불가능해도 backoff가 없어 연결/풀 초기화 실패가 계속 반복됩니다. 재시도가 연결 슬롯을 누수시킨다는 사실은 확인되지 않았지만 장애 중 접속 시도와 로그량을 증폭시킵니다. 감사 저널의 save/replay는 synchronized이고 내부에서 DB 저장을 시도하므로 느린 DB 연결 실패가 로그인·삭제 이력 기록까지 지연시킬 수 있습니다.

조치: 상한 있는 지수 backoff, 실패 로그 억제/복구 알림, 연결 대기 시간 제한, 연결 풀 예산과 저널 전송 작업 분리를 적용합니다. 감사 파일은 장애 중에도 보존합니다.

### F06 / P2: 저장장치 정책 조회 실패가 빈 목록으로 표시되고 등록 오류 원인이 바뀜

근거: `src/main/java/kr/kwater/hdcs/storage/service/impl/StorageServiceImpl.java:135`, `:201`, `:216`, `:293`

삭제 기준과 보관 기간 조회에서 모든 DataAccessException을 빈 목록으로 반환합니다. DB 연결 실패·컬럼 오류도 정상적인 데이터 없음처럼 보입니다. 보관/공간 정책 등록은 모든 DB 오류를 중복 값 오류로 바꿉니다. 따라서 사용자는 데이터가 삭제됐다고 오해하거나, 실제로는 연결·SQL 문제인데 이미 등록된 값이라는 안내를 받게 됩니다.

조치: 중복 제약 예외만 중복으로 처리하고, 조회·접속 실패는 오류 응답으로 전달합니다. 화면은 이전 목록을 보존하면서 조회 실패를 표시해야 합니다.

### F07 / P2: 일부 화면이 호출하는 백엔드 API가 프로젝트에 없음

근거: `src/main/resources/static/js/ftp/ftp.controller.js:11`, `src/main/resources/static/js/statistics/statistics.controller.js:10`, `src/main/resources/static/js/distribution/distribution.controller.js:9`, `src/main/resources/static/js/output/output.controller.js:10`, `src/main/resources/static/js/history/history.controller.js:16`

FTP·배포·산출물·이력·통계 화면은 각각 `/api/ftp`, `/api/distribution`, `/api/output`, `/api/history`, `/api/statistics`를 호출하지만 해당 서버 매핑을 현재 프로젝트에서 찾지 못했습니다. 다른 서비스를 프록시하는 설정도 이번 점검 범위에서 찾지 못했습니다. 이 프로젝트 단독 실행 기준으로 화면만 열리고 실제 조회·저장이 되지 않을 수 있습니다.

조치: 운영 범위에 포함된 화면은 API를 구현·연결하고, 범위 밖의 메뉴는 숨깁니다. 빈 목록으로 숨기는 오류 처리도 개선합니다.

### F08 / P2: 매핑 변경 뒤 이전 장비의 상태가 남아 허위 장애를 만들 수 있음

근거: `D:/Project/C26HDCS_Python/hdcs_db.py:321`, `src/main/resources/sql/python_device_status_postgresql.sql:41`

map-device는 매핑을 새 device_id로 변경하고 새 장비의 MONITOR 행을 추가하지만 이전 장비의 component 상태를 정리하지 않습니다. 웹 집계는 활성 매핑 확인 없이 component 상태가 있는 모든 장비를 갱신합니다. 예를 들어 같은 카메라를 다른 장비로 옮기면 이전 장비의 heartbeat가 만료되어 계속 ERROR와 시간 초과 알람을 생성할 수 있습니다. 운영 장비의 매핑을 바꾸어 재현하지는 않았으며 코드 경로에서 확인한 결함입니다.

조치: 매핑 이동/비활성화/목적지 폐기 시 이전 상태의 소유권을 정리하고 집계 대상을 활성 관리 장비로 한정합니다. 실제 전송 작업 이력은 삭제하지 않습니다.

### F09 / P2: 전체 애플리케이션 기동 테스트가 초기 SQL에서 실패함

근거: `src/main/resources/sql/schema.sql:34`, `src/main/resources/application-test.yml:12`

`HdcsWebServiceApplicationTests`를 test 프로필로 실행한 결과, TB_DEVICE가 없는 상태에서 TB_LOG가 이를 외래키로 참조해 기동이 실패했습니다. 개별 회귀 테스트 통과만으로 애플리케이션 전체 검증이 된 것은 아닙니다. 운영 설정은 SQL 초기화 mode=never이므로 이번 실패가 운영 테이블을 초기화한 것은 아닙니다.

조치: 테스트 전용 스키마의 의존 순서를 바로잡고 DB 구조·상태 함수까지 포함한 통합 테스트를 추가합니다. 운영 DB에서 초기화 schema.sql을 실행하면 안 됩니다.

## 추가 확인/시험 범위

- 알고리즘·품질 API와 매퍼에는 TB_ALGORITHM/TB_QUALITY 참조가 남아 있습니다. 대시보드의 불필요 조회 제거와는 별개입니다. 운영 DB 존재 여부는 슬롯 문제로 이번에 확인하지 못했습니다. 해당 모듈의 오래된 JS에는 /qos 경로도 남아 있으므로 활성화 시 정리해야 합니다.
- 환경설정의 IP/Port 저장·실통신 적용, 알람 경고음, 로그 저장 종료와 자동 리포트 생성 연결은 이전 시험절차서 보완 목록에 남아 있습니다.
- 기본 LibreOffice 실행 경로 파일은 이 PC에서 존재하지 않습니다. 별도 환경변수로 다른 실행 파일을 지정했는지와 실제 PDF 변환 성공은 추가 확인이 필요합니다.
- 자동 dump 삭제는 보관 기간과 폴더만 확인하며 Python에서 처리 완료된 dump인지 조회하지 않습니다. 수신 중인 폴더 대신 처리 완료 전용 폴더를 지정해야 합니다.

## 실행 검증 결과

- Java/H2/임시 폴더 회귀 테스트: 49개 통과. 이번 실행에서는 PostgreSQL 등록 테스트 1개를 비활성화하여 건너뛰었습니다.
- 별도 전체 Spring Boot 기동 테스트: 1개 실패. F09의 TB_DEVICE 생성 순서 오류를 재현했습니다.
- JavaScript 회귀 테스트: 7개 통과.
- Python SQLite/임시 폴더 회귀 테스트: 65개 중 50개 통과, PostgreSQL 선택 실행 테스트 15개 건너뜀.
- 운영 DB 접속: SQLSTATE 53300으로 거절되어 매퍼-운영 스키마 전수 대조 및 실제 운영 화면 연동 시험을 하지 못했습니다.
- 앞선 저장장치 수정의 PostgreSQL 등록 테스트는 이전 턴에서 로컬 시험 DB로 검증 완료했으며, 이번에 운영 DB에서 재실행한 것은 아닙니다.

검증 로그: `target/full-audit-java.log`, `target/full-audit-context.log`, `target/full-audit-python.log`.

## 권장 조치 순서

1. 운영 DB 접속 슬롯 확보 및 실제 연결 점유자 확인.
2. 공개 회원가입 역할 고정, 관리자 API 권한 제한, 운영 비밀 값 외부화/교체.
3. Python 수신 연결 상한/유휴 타임아웃 및 전체 DB 연결 예산 설정.
4. DB 실패 재시도와 저장장치 오류 응답 개선.
5. 테스트 스키마/전체 기동 테스트 복구 후 API·상태 수명주기 보완.
6. 운영 DB 구조 전수 대조 및 시험절차서 전체 시나리오 재검증.

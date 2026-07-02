-- QOS Web Service 초기 데이터

INSERT INTO TB_QUALITY (NAME, STATUS, PROGRESS) VALUES
('처리작업-001', '처리중', 75),
('처리작업-002', '완료',   100),
('처리작업-003', '대기',   0),
('처리작업-004', '처리중', 40),
('처리작업-005', '대기',   0);

INSERT INTO TB_ALGORITHM (NAME, TYPE, ENABLED) VALUES
('QOS-ALG-001', '최적화', 'Y'),
('QOS-ALG-002', '분류',   'Y'),
('QOS-ALG-003', '예측',   'N'),
('QOS-ALG-004', '분석',   'Y'),
('QOS-ALG-005', '필터링', 'N');

INSERT INTO TB_LOG (LEVEL, MESSAGE, REG_DT) VALUES
('INFO',  '시스템 시작',       '2026-05-11 09:00:00'),
('WARN',  '처리 지연 감지',     '2026-05-11 10:30:00'),
('ERROR', '연결 오류 발생',     '2026-05-11 11:45:00'),
('INFO',  '알고리즘 실행 완료', '2026-05-11 13:00:00'),
('DEBUG', '파라미터 확인',      '2026-05-11 14:00:00');

INSERT INTO TB_ALARM (TYPE, TARGET, SEVERITY, STATUS, REG_DT) VALUES
('임계값 초과', 'NODE-01', 'HIGH',   '미처리',  '2026-05-11 10:00:00'),
('연결 끊김',   'NODE-05', 'MEDIUM', '처리중',  '2026-05-11 11:00:00'),
('응답 지연',   'NODE-03', 'LOW',    '처리완료', '2026-05-11 12:00:00');

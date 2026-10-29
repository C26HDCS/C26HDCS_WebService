'use strict';

qosApp.controller('ReportCtrl', ['$scope', '$http', '$timeout', 'toast', function ($scope, $http, $timeout, toast) {
    $scope.reportList = [];
    $scope.searchReportNo = '';
    $scope.searchKeyword = '';
    $scope.searchGenerateType = '';
    $scope.searchDataType = '';
    $scope.generating = false;

    function toDateStr(v) {
        if (!v) return '';
        if (typeof v === 'string') return v.substring(0, 10);
        return v.getFullYear() + '-' +
            String(v.getMonth() + 1).padStart(2, '0') + '-' +
            String(v.getDate()).padStart(2, '0');
    }

    // ── 페이징 ──
    var PAGE_SIZE = 10;
    $scope.pageSize    = PAGE_SIZE;
    $scope.pagedList   = [];
    $scope.currentPage = 1;
    $scope.totalPages  = 1;
    $scope.pageNums    = [1];

    function updatePaging() {
        var total = $scope.reportList.length;
        $scope.totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
        if ($scope.currentPage < 1) $scope.currentPage = 1;
        if ($scope.currentPage > $scope.totalPages) $scope.currentPage = $scope.totalPages;

        var start = ($scope.currentPage - 1) * PAGE_SIZE;
        $scope.pagedList = $scope.reportList.slice(start, start + PAGE_SIZE);

        var half = 2;
        var from = Math.max(1, $scope.currentPage - half);
        var to   = Math.min($scope.totalPages, $scope.currentPage + half);
        if (to - from < 4) {
            if (from === 1) to = Math.min($scope.totalPages, from + 4);
            else            from = Math.max(1, to - 4);
        }
        var pages = [];
        for (var i = from; i <= to; i++) pages.push(i);
        $scope.pageNums = pages;
    }

    $scope.goToPage = function (n) {
        if (n < 1 || n > $scope.totalPages || n === $scope.currentPage) return;
        $scope.currentPage = n;
        updatePaging();
    };

    function emptyGen() {
        return { dateFrom: null, dateTo: null, typeLog: false, typeEvent: false, typeAlarm: false, typeData: false };
    }

    $scope.gen = emptyGen();

    // ── 기간 제한 (서버 report.max-period-days 와 같은 값으로 맞출 것) ──
    // 종료일은 오늘까지, 전체 기간은 최대 MAX_PERIOD_DAYS일. 서버에서도 같은 검사를 하므로 여기는 안내용이다.
    var MAX_PERIOD_DAYS = 31;
    $scope.maxPeriodDays = MAX_PERIOD_DAYS;

    function pad2(n) { return String(n).padStart(2, '0'); }

    function todayStr() {
        return toDateStr(new Date());
    }

    // 'YYYY-MM-DD' 문자열에 날짜를 더하고 뺀 결과를 같은 형식으로 돌려준다.
    function addDays(str, n) {
        var p = str.split('-');
        var d = new Date(+p[0], +p[1] - 1, +p[2] + n);
        return d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate());
    }

    function daysBetween(from, to) {
        var a = from.split('-'), b = to.split('-');
        var ms = new Date(+b[0], +b[1] - 1, +b[2]) - new Date(+a[0], +a[1] - 1, +a[2]);
        return Math.round(ms / 86400000) + 1;
    }

    // 시작일: 종료일이 정해져 있으면 그 날짜 기준 최대 기간 안쪽만 선택 가능
    $scope.genFromMin = function () {
        var to = toDateStr($scope.gen.dateTo);
        return to ? addDays(to, -(MAX_PERIOD_DAYS - 1)) : '';
    };
    $scope.genFromMax = function () {
        var to = toDateStr($scope.gen.dateTo);
        var today = todayStr();
        return (to && to < today) ? to : today;
    };
    // 종료일: 시작일 이후, 오늘 이전, 시작일 기준 최대 기간 안쪽만 선택 가능
    $scope.genToMin = function () {
        return toDateStr($scope.gen.dateFrom);
    };
    $scope.genToMax = function () {
        var from = toDateStr($scope.gen.dateFrom);
        var today = todayStr();
        if (!from) return today;
        var limit = addDays(from, MAX_PERIOD_DAYS - 1);
        return limit < today ? limit : today;
    };
    // 선택한 기간의 일수 (둘 다 선택되고 올바른 순서일 때만)
    $scope.genDays = function () {
        var from = toDateStr($scope.gen.dateFrom);
        var to   = toDateStr($scope.gen.dateTo);
        return (from && to && from <= to) ? daysBetween(from, to) : 0;
    };

    // 데이터 유형 "전체" 체크박스: 4개가 모두 선택돼 있으면 체크 상태, 누르면 전체 선택/해제
    $scope.isAllTypes = function () {
        var g = $scope.gen;
        return g.typeLog && g.typeEvent && g.typeAlarm && g.typeData;
    };

    $scope.toggleAllTypes = function () {
        var on = !$scope.isAllTypes();
        $scope.gen.typeLog = $scope.gen.typeEvent = $scope.gen.typeAlarm = $scope.gen.typeData = on;
    };

    // 입력 검증 안내: 생성 영역 안에 문구로 보여 주고, 입력을 고치면 지운다.
    $scope.genError = '';
    $scope.$watch('gen', function () { $scope.genError = ''; }, true);

    $scope.resetGen = function () {
        $scope.gen = emptyGen();
        $scope.genError = '';
    };

    $scope.generateReport = function () {
        if ($scope.generating) return;

        var from = toDateStr($scope.gen.dateFrom);
        var to   = toDateStr($scope.gen.dateTo);
        $scope.genError = '';
        if (!from || !to) {
            $scope.genError = '조회 기간(시작일, 종료일)을 선택해 주세요.';
            return;
        }
        if (from > to) {
            $scope.genError = '시작일은 종료일보다 클 수 없습니다.';
            return;
        }
        if (to > todayStr()) {
            $scope.genError = '종료일은 오늘 이후로 선택할 수 없습니다.';
            return;
        }
        if (daysBetween(from, to) > MAX_PERIOD_DAYS) {
            $scope.genError = '조회 기간은 최대 ' + MAX_PERIOD_DAYS + '일까지 선택할 수 있습니다.';
            return;
        }

        var dataTypes = [];
        if ($scope.gen.typeLog)   dataTypes.push('LOG');
        if ($scope.gen.typeEvent) dataTypes.push('EVENT');
        if ($scope.gen.typeAlarm) dataTypes.push('ALARM');
        if ($scope.gen.typeData)  dataTypes.push('DATA');
        if (dataTypes.length === 0) {
            $scope.genError = '데이터 유형을 1개 이상 선택해 주세요.';
            return;
        }

        var payload = { periodFrom: from, periodTo: to, dataTypes: dataTypes };

        // 같은 기간·유형의 리포트가 이미 있으면 바로 만들지 않고 확인 모달을 띄운다.
        // 확인 요청 자체가 실패해도 생성을 막지는 않는다. (중복 확인은 안내용)
        $scope.generating = true;
        $http.get(ctx + '/api/report/list')
            .then(function (res) {
                var dups = findDuplicates(payload, res.data || []);
                if (dups.length === 0) {
                    submitGenerate(payload);
                    return;
                }
                $scope.generating = false;
                $scope.dupMatches   = dups;
                $scope.dupInfo      = { from: from, to: to, types: dataTypes.join(',') };
                pendingPayload      = payload;
                $scope.showDupModal = true;
            })
            .catch(function () {
                submitGenerate(payload);
            });
    };

    // ── 같은 조건(기간·데이터 유형) 중복 확인 ──
    $scope.showDupModal = false;
    $scope.dupMatches   = [];
    $scope.dupInfo      = null;
    var pendingPayload  = null;

    // 기간이 같고 데이터 유형 구성이 같은(순서 무관) 기존 리포트를 찾는다. 수동·자동 생성분을 모두 본다.
    function findDuplicates(payload, list) {
        var want = payload.dataTypes.slice().sort().join(',');
        return list.filter(function (r) {
            if (r.periodFrom !== payload.periodFrom || r.periodTo !== payload.periodTo) return false;
            var have = (r.dataTypes || '').split(',')
                .map(function (c) { return c.trim(); })
                .filter(function (c) { return c; })
                .sort().join(',');
            return have === want;
        });
    }

    $scope.confirmDupGenerate = function () {
        var payload = pendingPayload;
        $scope.showDupModal = false;
        pendingPayload = null;
        if (payload) submitGenerate(payload);
    };

    $scope.cancelDupGenerate = function () {
        $scope.showDupModal = false;
        $scope.dupMatches   = [];
        pendingPayload      = null;
    };

    function submitGenerate(payload) {
        $scope.generating = true;
        $http.post(ctx + '/api/report', payload)
            .then(function (res) {
                $scope.generating = false;
                toast.success('리포트가 생성되었습니다.', res.data.reportNo + ' · ' + res.data.reportName);
                $scope.resetGen();
                // 검색 조건에 걸려 새 리포트가 목록에서 빠지지 않도록 검색 조건을 비우고, 새 행을 강조한다.
                $scope.searchReportNo = '';
                $scope.searchKeyword = '';
                $scope.searchGenerateType = '';
                $scope.searchDataType = '';
                $scope.searchDateFrom = null;
                $scope.searchDateTo = null;
                $scope.load(res.data.reportId);
            })
            .catch(function (err) {
                $scope.generating = false;
                var msg = err && err.data && err.data.message;
                toast.error(msg || '리포트 생성 중 오류가 발생했습니다.');
            });
    }

    // 새로 생성된 리포트 행 강조 (몇 초 뒤 자동으로 사라진다)
    var HIGHLIGHT_MS = 3500;
    var highlightTimer = null;
    $scope.highlightId = null;

    function highlightReport(reportId) {
        var idx = -1;
        for (var i = 0; i < $scope.reportList.length; i++) {
            if ($scope.reportList[i].reportId === reportId) { idx = i; break; }
        }
        if (idx < 0) return;

        // 새 행이 있는 페이지로 이동
        $scope.currentPage = Math.floor(idx / PAGE_SIZE) + 1;
        updatePaging();

        if (highlightTimer) $timeout.cancel(highlightTimer);
        $scope.highlightId = reportId;

        // 화면에 그려진 뒤, 보이지 않는 위치에 있으면 스크롤해서 보여준다.
        $timeout(function () {
            var row = document.querySelector('.report-row-new');
            if (row && row.scrollIntoView) row.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
        }, 0);
        highlightTimer = $timeout(function () { $scope.highlightId = null; }, HIGHLIGHT_MS);
    }

    $scope.loadError = '';

    // 입력 검증 안내: 검색 영역 안에 문구로 보여 주고, 날짜를 고치면 지운다.
    $scope.searchError = '';
    $scope.$watchGroup(['searchDateFrom', 'searchDateTo'], function () { $scope.searchError = ''; });

    // 조회 기간: 종료일은 시작일보다 앞설 수 없고, 시작일은 종료일보다 뒤일 수 없게 달력에서 막는다.
    $scope.searchFromMax = function () { return toDateStr($scope.searchDateTo); };
    $scope.searchToMin   = function () { return toDateStr($scope.searchDateFrom); };

    $scope.load = function (newReportId) {
        var fromStr = toDateStr($scope.searchDateFrom);
        var toStr   = toDateStr($scope.searchDateTo);
        $scope.searchError = '';
        if (fromStr && toStr && fromStr > toStr) {
            $scope.searchError = '시작일은 종료일보다 클 수 없습니다.';
            return;
        }

        $scope.loading = true;
        $http.get(ctx + '/api/report/list', {
            params: {
                searchReportNo:     $scope.searchReportNo || '',
                searchKeyword:      $scope.searchKeyword || '',
                searchGenerateType: $scope.searchGenerateType || '',
                searchDataType:     $scope.searchDataType || '',
                searchDateFrom:     fromStr,
                searchDateTo:       toStr
            }
        })
            .then(function (res) {
                $scope.reportList  = res.data;
                $scope.loadError = '';
                $scope.currentPage = 1;
                updatePaging();
                if (typeof newReportId === 'number') highlightReport(newReportId);
            })
            .catch(function () {
                $scope.loadError = '목록을 불러오지 못했습니다. 서버 연결을 확인한 뒤 다시 시도해 주세요.';
                $scope.reportList = [];
                $scope.currentPage = 1;
                updatePaging();
            })
            .finally(function () { $scope.loading = false; });
    };

    // ── 상세 정보 모달 ──
    var TYPE_LABELS = { LOG: '로그', EVENT: '이벤트', ALARM: '알람', DATA: '수신데이터' };

    $scope.showDetailModal = false;
    $scope.detailReport    = {};

    // 데이터 유형 코드(LOG,EVENT,...)를 한글 라벨 배열로 변환 (태그 표시용)
    $scope.getDataTypeLabels = function (dataTypes) {
        if (!dataTypes) return [];
        return dataTypes.split(',').map(function (c) {
            return TYPE_LABELS[c.trim()] || c.trim();
        });
    };

    $scope.formatRegDt = function (regDt) {
        if (!regDt) return '-';
        return regDt.replace('T', ' ').substring(0, 16);
    };

    $scope.formatFileSize = function (bytes) {
        if (bytes === null || bytes === undefined) return '-';
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    };

    // 목록 행 클릭 시 서버에서 최신 상세 정보(생성자 이름 포함)를 가져와 모달을 연다.
    $scope.openDetailModal = function (item) {
        $http.get(ctx + '/api/report/' + item.reportId)
            .then(function (res) {
                $scope.detailReport = res.data;
                $scope.showDetailModal = true;
            })
            .catch(function (err) {
                var msg = err && err.data && err.data.message;
                toast.error(msg || '리포트 상세 정보를 불러오지 못했습니다.');
                if (err && err.status === 404) $scope.load();
            });
    };

    // ── Word / PDF 다운로드 ──
    // 인증 토큰이 요청 헤더로만 전달되므로(링크 이동 불가) $http 로 받아서 Blob으로 저장한다.
    // PDF는 처음 받을 때 서버에서 변환하므로 시간이 걸릴 수 있다.
    // 진행 중에는 같은 버튼을 다시 누르지 못하게 하고, 0.6초가 지나도 끝나지 않으면 버튼의 아이콘을 스피너로 바꿔 보여준다.
    // (이미 변환된 파일은 바로 받아져서 깜빡이지 않도록 약간 늦게 보여준다)
    var SLOW_MS = 600;
    var DOWNLOADS = {
        word: { path: 'word', mime: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', ext: 'docx' },
        pdf:  { path: 'pdf',  mime: 'application/pdf', ext: 'pdf' }
    };

    $scope.downloadFile = function (item, kind) {
        var d = DOWNLOADS[kind];
        item.dl = item.dl || {};
        if (!d || item.dl[kind]) return;
        item.dl[kind] = true;
        var slowTimer = $timeout(function () { item.dl[kind + 'Slow'] = true; }, SLOW_MS);
        $http.get(ctx + '/api/report/' + item.reportId + '/download/' + d.path, { responseType: 'blob' })
            .then(function (res) {
                var blob = new Blob([res.data], { type: d.mime });
                var name = (item.fileName || ('report_' + item.reportId + '.docx'))
                    .replace(/\.[^.]*$/, '') + '.' + d.ext;
                var url = URL.createObjectURL(blob);
                var a = document.createElement('a');
                a.href = url;
                a.download = name;
                document.body.appendChild(a);
                a.click();
                document.body.removeChild(a);
                setTimeout(function () { URL.revokeObjectURL(url); }, 1000);
            })
            .catch(function (err) {
                // blob 응답이라 에러 메시지(JSON)도 Blob으로 온다. 읽어서 보여준다.
                var fallback = '리포트 다운로드 중 오류가 발생했습니다.';
                if (err && err.data instanceof Blob) {
                    var reader = new FileReader();
                    reader.onload = function () {
                        var msg = fallback;
                        try { msg = JSON.parse(reader.result).message || fallback; } catch (e) {}
                        toast.error(msg);
                    };
                    reader.readAsText(err.data);
                } else {
                    toast.error(fallback);
                }
            })
            .finally(function () {
                $timeout.cancel(slowTimer);
                item.dl[kind] = false;
                item.dl[kind + 'Slow'] = false;
            });
    };

    $scope.closeDetailModal = function () {
        $scope.showDetailModal = false;
        $scope.detailReport = {};
    };

    $scope.load();
}]);

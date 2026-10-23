'use strict';

qosApp.controller('LogCtrl', ['$scope', '$http', function ($scope, $http) {

    $scope.logList        = [];
    $scope.filterCategory = '';   // '' (전체) / 'LOG' / 'EVENT'
    $scope.filterLevel    = null; // null (전체) / 로그 레벨 / 이벤트 유형  ※ ''가 아니라 null 이어야 select 의 '전체' 항목이 선택돼 보인다.
    $scope.searchKeyword  = '';
    $scope.searchDateFrom = '';
    $scope.searchDateTo   = '';

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
    $scope.totalPages  = 0;
    $scope.pageNums    = [];

    function updatePaging() {
        var total = $scope.logList.length;
        $scope.totalPages = Math.ceil(total / PAGE_SIZE);
        if ($scope.currentPage < 1) $scope.currentPage = 1;
        if ($scope.totalPages > 0 && $scope.currentPage > $scope.totalPages) {
            $scope.currentPage = $scope.totalPages;
        }
        var start = ($scope.currentPage - 1) * PAGE_SIZE;
        $scope.pagedList = $scope.logList.slice(start, start + PAGE_SIZE);

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

    $scope.loadError = '';

    // 구분을 바꾸면, 선택돼 있던 유형이 그 구분에 없는 값일 때 '전체'로 되돌린다.
    var LOG_OPTIONS = [
        { value: 'INFO',  label: 'INFO',  group: '로그' },
        { value: 'WARN',  label: 'WARN',  group: '로그' },
        { value: 'ERROR', label: 'ERROR', group: '로그' },
        { value: 'DEBUG', label: 'DEBUG', group: '로그' }
    ];
    var EVENT_OPTIONS = [
        { value: 'LOGIN_SUCCESS', label: '로그인 성공', group: '이벤트' },
        { value: 'LOGIN_FAIL',    label: '로그인 실패', group: '이벤트' },
        { value: 'LOGOUT',        label: '로그아웃',    group: '이벤트' },
        { value: 'STORAGE_DELETE_REQUEST', label: '파일 삭제 요청', group: '이벤트' },
        { value: 'STORAGE_DELETE_SUCCESS', label: '파일 삭제 완료', group: '이벤트' },
        { value: 'STORAGE_DELETE_FAIL', label: '파일 삭제 실패', group: '이벤트' },
        { value: 'STORAGE_DELETE_SKIPPED', label: '파일 삭제 제외', group: '이벤트' },
        { value: 'STORAGE_CLEANUP_DONE', label: '공간 정리 완료', group: '이벤트' },
        { value: 'STORAGE_CLEANUP_FAIL', label: '공간 점검 실패', group: '이벤트' },
        { value: 'STORAGE_SPACE_LOW', label: '가용 공간 부족', group: '이벤트' },
        { value: 'REPORT_CREATE_SUCCESS', label: '리포트 생성 성공', group: '이벤트' },
        { value: 'REPORT_CREATE_FAIL',    label: '리포트 생성 실패', group: '이벤트' }
    ];

    // 유형 선택 목록: 구분이 '로그'면 로그 레벨만, '이벤트'면 이벤트 유형만, '전체'면 둘 다 보여 준다.
    function refreshLevelOptions() {
        var opts = [];
        if ($scope.filterCategory !== 'EVENT') opts = opts.concat(LOG_OPTIONS);
        if ($scope.filterCategory !== 'LOG')   opts = opts.concat(EVENT_OPTIONS);
        $scope.levelOptions = opts;
        // 선택돼 있던 유형이 새 목록에 없으면 '전체'로 되돌린다.
        var stillThere = opts.some(function (o) { return o.value === $scope.filterLevel; });
        if (!stillThere) $scope.filterLevel = null;
    }

    $scope.onCategoryChange = refreshLevelOptions;
    refreshLevelOptions();

    $scope.load = function () {
        var fromStr = toDateStr($scope.searchDateFrom);
        var toStr   = toDateStr($scope.searchDateTo);

        if (fromStr && toStr && fromStr > toStr) {
            alert('시작일은 종료일보다 클 수 없습니다.');
            return;
        }

        $http.get(ctx + '/api/log/list', {
            params: {
                searchCondition: $scope.filterCategory,
                searchLevel:     $scope.filterLevel,
                searchKeyword:   $scope.searchKeyword,
                searchDateFrom:  fromStr,
                searchDateTo:    toStr
            }
        })
            .then(function (res) {
                $scope.logList = res.data;
                $scope.loadError = '';
                $scope.currentPage = 1;
                updatePaging();
            })
            .catch(function () {
                $scope.loadError = '목록을 불러오지 못했습니다. 서버 연결을 확인한 뒤 다시 시도해 주세요.';
                $scope.logList = [];
                $scope.currentPage = 1;
                updatePaging();
            });
    };

    // 구분(로그/이벤트) 배지
    $scope.getCategoryLabel = function (category) {
        return category === 'EVENT' ? '이벤트' : '로그';
    };
    $scope.getCategoryClass = function (category) {
        return category === 'EVENT' ? 'log-badge-event' : 'log-badge-device';
    };

    // 유형 배지: 로그면 레벨(INFO/WARN/ERROR/DEBUG), 이벤트면 이벤트 유형(LOGIN_SUCCESS 등)
    $scope.getLevelClass = function (level) {
        var map = {
            'INFO':          'log-badge-info',
            'WARN':          'log-badge-warn',
            'ERROR':         'log-badge-error',
            'DEBUG':         'log-badge-debug',
            'LOGIN_SUCCESS': 'log-badge-info',
            'LOGIN_FAIL':    'log-badge-error',
            'LOGOUT':        'log-badge-debug',
            'STORAGE_DELETE_REQUEST': 'log-badge-info',
            'STORAGE_DELETE_SUCCESS': 'log-badge-info',
            'STORAGE_DELETE_FAIL': 'log-badge-error',
            'STORAGE_DELETE_SKIPPED': 'log-badge-warn',
            'STORAGE_CLEANUP_DONE': 'log-badge-info',
            'STORAGE_CLEANUP_FAIL': 'log-badge-error',
            'STORAGE_SPACE_LOW': 'log-badge-warn',
            'REPORT_CREATE_SUCCESS': 'log-badge-info',
            'REPORT_CREATE_FAIL':    'log-badge-error'
        };
        return map[level] || 'log-badge-debug';
    };

    // 유형 배지 글자: 이벤트 유형의 결과 접미사(_SUCCESS/_FAIL)는 색(초록/빨강)으로 구분되므로 글자에서는 뺀다.
    // 예) LOGIN_SUCCESS → LOGIN(초록), LOGIN_FAIL → LOGIN(빨강)
    // 리포트는 생성 외 이벤트가 없어 REPORT_CREATE_SUCCESS/FAIL 을 REPORT 로 줄여 보여 준다. (저장 값은 그대로)
    $scope.getLevelLabel = function (level) {
        var storageLabels = {
            'STORAGE_DELETE_REQUEST': '삭제 요청',
            'STORAGE_DELETE_SUCCESS': '삭제 완료',
            'STORAGE_DELETE_FAIL': '삭제 실패',
            'STORAGE_DELETE_SKIPPED': '삭제 제외',
            'STORAGE_CLEANUP_DONE': '정리 완료',
            'STORAGE_CLEANUP_FAIL': '점검 실패',
            'STORAGE_SPACE_LOW': '공간 부족'
        };
        if (storageLabels[level]) return storageLabels[level];
        return level ? String(level).replace(/_(SUCCESS|FAIL)$/, '') : level;
    };

    $scope.getTarget = function (item) {
        if (item.category === 'EVENT') return item.userId || '-';
        return item.deviceName || item.deviceId || '-';
    };

    $scope.formatRegDt = function (regDt) {
        if (!regDt) return '-';
        return regDt.replace('T', ' ').substring(0, 16);
    };

    $scope.showDetailModal = false;
    $scope.detailLog       = {};

    $scope.openDetailModal = function (item) {
        $scope.detailLog = angular.copy(item);
        $scope.showDetailModal = true;
    };

    $scope.closeDetailModal = function () {
        $scope.showDetailModal = false;
        $scope.detailLog = {};
    };

    $scope.load();
}]);

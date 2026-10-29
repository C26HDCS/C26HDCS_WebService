'use strict';

qosApp.controller('AlarmCtrl', ['$scope', '$http', 'toast', function ($scope, $http, toast) {

    // 헤더 알림 드롭다운 등 다른 곳에서 확인 처리된 알람을 이 목록에도 반영 (페이지 유지, 재조회 없음)
    $scope.$on('alarmChecked', function (event, alarmId) {
        var found = $scope.alarmList.filter(function (a) { return a.id === alarmId; })[0];
        if (found) found.checked = true;
    });

    $scope.alarmList       = [];
    $scope.searchAlarmType  = '';
    $scope.searchAlarmLevel = '';
    $scope.searchKeyword    = '';   // 장비명 또는 메시지
    $scope.searchReadStatus = '';   // '' (전체) / 'UNREAD' (안 읽은 알람)
    $scope.searchDateFrom   = '';
    $scope.searchDateTo     = '';

    function toDateStr(v) {
        if (!v) return '';
        if (typeof v === 'string') return v.substring(0, 10);
        return v.getFullYear() + '-' +
            String(v.getMonth() + 1).padStart(2, '0') + '-' +
            String(v.getDate()).padStart(2, '0');
    }

    var PAGE_SIZE = 10;
    $scope.pageSize    = PAGE_SIZE;
    $scope.pagedList   = [];
    $scope.currentPage = 1;
    $scope.totalPages  = 0;
    $scope.pageNums    = [];

    function updatePaging() {
        var total = $scope.alarmList.length;
        $scope.totalPages = Math.ceil(total / PAGE_SIZE);
        if ($scope.currentPage < 1) $scope.currentPage = 1;
        if ($scope.totalPages > 0 && $scope.currentPage > $scope.totalPages) {
            $scope.currentPage = $scope.totalPages;
        }
        var start = ($scope.currentPage - 1) * PAGE_SIZE;
        $scope.pagedList = $scope.alarmList.slice(start, start + PAGE_SIZE);

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

    // 입력 검증 안내: 검색 영역 안에 문구로 보여 주고, 날짜를 고치면 지운다.
    $scope.searchError = '';
    $scope.$watchGroup(['searchDateFrom', 'searchDateTo'], function () { $scope.searchError = ''; });

    // 조회 기간: 종료일은 시작일보다 앞설 수 없고, 시작일은 종료일보다 뒤일 수 없게 달력에서 막는다.
    $scope.searchFromMax = function () { return toDateStr($scope.searchDateTo); };
    $scope.searchToMin   = function () { return toDateStr($scope.searchDateFrom); };

    $scope.load = function (keepPage) {
        var fromStr = toDateStr($scope.searchDateFrom);
        var toStr   = toDateStr($scope.searchDateTo);

        $scope.searchError = '';
        if (fromStr && toStr && fromStr > toStr) {
            $scope.searchError = '시작일은 종료일보다 클 수 없습니다.';
            return;
        }

        var params = {
            searchAlarmType:  $scope.searchAlarmType,
            searchAlarmLevel: $scope.searchAlarmLevel,
            searchKeyword:    $scope.searchKeyword,
            searchDateFrom:   fromStr,
            searchDateTo:     toStr
        };
        if ($scope.searchReadStatus === 'UNREAD') params.searchUnreadOnly = true;

        // 15초 폴링·SSE 로 인한 백그라운드 갱신(keepPage)에는 로딩 표시를 하지 않는다.
        if (!keepPage) $scope.loading = true;
        $http.get(ctx + '/api/alarm/list', { params: params })
            .then(function (res) {
                $scope.alarmList = res.data;
                $scope.loadError = '';
                if (!keepPage) $scope.currentPage = 1;
                updatePaging();
            })
            .catch(function () {
                $scope.loadError = '목록을 불러오지 못했습니다. 서버 연결을 확인한 뒤 다시 시도해 주세요.';
                $scope.alarmList = [];
                $scope.currentPage = 1;
                updatePaging();
            })
            .finally(function () { $scope.loading = false; });
    };

    // 안 읽은 알람을 모두 읽음 처리
    $scope.checkingAll = false;
    $scope.showCheckAllModal = false;

    // '모두 읽음 처리' 버튼: 바로 처리하지 않고 확인 모달을 띄운다.
    $scope.checkAll = function () {
        if ($scope.checkingAll || !$scope.unreadCount) return;
        $scope.showCheckAllModal = true;
    };

    $scope.cancelCheckAll = function () {
        $scope.showCheckAllModal = false;
    };

    $scope.confirmCheckAll = function () {
        $scope.showCheckAllModal = false;
        if ($scope.checkingAll) return;

        $scope.checkingAll = true;
        $http.post(ctx + '/api/alarm/check-all')
            .then(function (res) {
                // 헤더의 안 읽은 개수는 부모(AppCtrl)가 갖고 있으므로 이벤트로 알린다.
                $scope.$emit('alarmAllChecked', res.data.unreadCount);
                if ($scope.searchReadStatus === 'UNREAD') {
                    $scope.load();                       // 안 읽은 알람만 보는 중이면 목록이 비어야 한다.
                } else {
                    $scope.alarmList.forEach(function (a) { a.checked = true; });   // 페이지·검색 상태 유지
                }
            })
            .catch(function () {
                toast.error('모두 읽음 처리 중 오류가 발생했습니다.');
            })
            .finally(function () {
                $scope.checkingAll = false;
            });
    };

    // getLevelClass / formatOccurredAt / openDetailModal / closeDetailModal 은
    // 부모 스코프인 AppCtrl(app.js)에 공통으로 정의되어 있어 그대로 상속받아 사용함
    $scope.rowClass = function (item) {
        return item.checked ? 'alarm-row-read' : 'alarm-row-unread';
    };

    $scope.$on('alarmDataChanged', function () { $scope.load(true); });
    $scope.load();
}]);

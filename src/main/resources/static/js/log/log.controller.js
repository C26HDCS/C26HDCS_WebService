'use strict';

qosApp.controller('LogCtrl', ['$scope', '$http', function ($scope, $http) {

    $scope.logList        = [];
    $scope.filterCategory = '';   // '' (전체) / 'LOG' / 'EVENT'
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
                searchKeyword:   $scope.searchKeyword,
                searchDateFrom:  fromStr,
                searchDateTo:    toStr
            }
        })
            .then(function (res) {
                $scope.logList = res.data;
                $scope.currentPage = 1;
                updatePaging();
            })
            .catch(function () {
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
            'LOGOUT':        'log-badge-debug'
        };
        return map[level] || 'log-badge-debug';
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

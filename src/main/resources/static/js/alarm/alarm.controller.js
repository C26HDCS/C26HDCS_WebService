'use strict';

qosApp.controller('AlarmCtrl', ['$scope', '$http', function ($scope, $http) {

    // 헤더 알림 드롭다운 등 다른 곳에서 확인 처리된 알람을 이 목록에도 반영 (페이지 유지, 재조회 없음)
    $scope.$on('alarmChecked', function (event, alarmId) {
        var found = $scope.alarmList.filter(function (a) { return a.id === alarmId; })[0];
        if (found) found.checked = true;
    });

    $scope.alarmList       = [];
    $scope.searchAlarmType  = '';
    $scope.searchAlarmLevel = '';
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

    $scope.load = function () {
        var fromStr = toDateStr($scope.searchDateFrom);
        var toStr   = toDateStr($scope.searchDateTo);

        if (fromStr && toStr && fromStr > toStr) {
            alert('시작일은 종료일보다 클 수 없습니다.');
            return;
        }

        $http.get(ctx + '/api/alarm/list', {
            params: {
                searchAlarmType:  $scope.searchAlarmType,
                searchAlarmLevel: $scope.searchAlarmLevel,
                searchDateFrom:   fromStr,
                searchDateTo:     toStr
            }
        })
            .then(function (res) {
                $scope.alarmList = res.data;
                $scope.currentPage = 1;
                updatePaging();
            })
            .catch(function () {
                $scope.alarmList = [];
                $scope.currentPage = 1;
                updatePaging();
            });
    };

    // getLevelClass / formatOccurredAt / openDetailModal / closeDetailModal 은
    // 부모 스코프인 AppCtrl(app.js)에 공통으로 정의되어 있어 그대로 상속받아 사용함
    $scope.rowClass = function (item) {
        return item.checked ? 'alarm-row-read' : 'alarm-row-unread';
    };

    $scope.load();
}]);

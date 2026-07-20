'use strict';

qosApp.controller('DashboardCtrl', ['$scope', '$http', '$interval', function ($scope, $http, $interval) {

    $scope.summary = { totalDevices: 0, normal: 0, fault: 0, warn: 0 };
    $scope.devices  = [];
    $scope.currentTime = '';
    $scope.equipPanelCollapsed = false;

    $scope.toggleEquipPanel = function () {
        $scope.equipPanelCollapsed = !$scope.equipPanelCollapsed;
    };

    $scope.statusLabel = function (status) {
        if (status === 'NORMAL')  return '정상';
        if (status === 'WARNING') return '경고';
        if (status === 'ERROR')   return '장애';
        return status || '-';
    };

    function pad(n) { return n < 10 ? '0' + n : n; }
    function updateTime() {
        var now = new Date();
        $scope.currentTime =
            now.getFullYear() + '-' + pad(now.getMonth() + 1) + '-' + pad(now.getDate()) +
            ' ' + pad(now.getHours()) + ':' + pad(now.getMinutes()) + ':' + pad(now.getSeconds()) + ' KST';
    }
    updateTime();
    $interval(updateTime, 1000);

    $http.get(ctx + '/api/dashboard/summary')
        .then(function (res) {
            $scope.summary = res.data;
        })
        .catch(function () {
            $scope.summary = { totalDevices: 80, normal: 75, fault: 5, warn: 5 };
        });

    $http.get(ctx + '/api/dashboard/devices')
        .then(function (res) {
            $scope.devices = res.data;
        });
}]);

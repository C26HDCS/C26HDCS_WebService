'use strict';

qosApp.controller('AlarmCtrl', ['$scope', '$http', function ($scope, $http) {

    $scope.alarmList    = [];
    $scope.filterStatus = '';

    $scope.load = function () {
        $http.get('/qos/api/alarm/list')
            .then(function (res) { $scope.alarmList = res.data; })
            .catch(function ()   { $scope.alarmList = []; });
    };

    $scope.getSeverityClass = function (severity) {
        var map = { 'HIGH': 'status-high', 'MEDIUM': 'status-medium', 'LOW': 'status-low' };
        return map[severity] || '';
    };

    $scope.filteredList = function () {
        if (!$scope.filterStatus) return $scope.alarmList;
        return $scope.alarmList.filter(function (a) { return a.status === $scope.filterStatus; });
    };

    $scope.load();
}]);

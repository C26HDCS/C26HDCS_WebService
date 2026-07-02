'use strict';

qosApp.controller('LogCtrl', ['$scope', '$http', function ($scope, $http) {

    $scope.logList      = [];
    $scope.filterLevel  = '';

    $scope.load = function () {
        $http.get(ctx + '/api/log/list')
            .then(function (res) { $scope.logList = res.data; })
            .catch(function ()   { $scope.logList = []; });
    };

    $scope.getLevelClass = function (level) {
        var map = {
            'INFO':  'status-badge log-badge-info',
            'WARN':  'status-badge log-badge-warn',
            'ERROR': 'status-badge log-badge-error',
            'DEBUG': 'status-badge log-badge-debug'
        };
        return map[level] || 'status-badge';
    };

    $scope.filteredList = function () {
        if (!$scope.filterLevel) return $scope.logList;
        return $scope.logList.filter(function (l) { return l.level === $scope.filterLevel; });
    };

    $scope.load();
}]);

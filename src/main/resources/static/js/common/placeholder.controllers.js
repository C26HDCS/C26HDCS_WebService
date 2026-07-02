'use strict';

qosApp.controller('ConfigCtrl',  ['$scope', function ($scope) {}]);
qosApp.controller('StorageCtrl', ['$scope', function ($scope) {}]);
qosApp.controller('ReportCtrl', ['$scope', '$http', function ($scope, $http) {
    $scope.reportList = [];
    $scope.searchKeyword = '';
    $scope.searchFormat  = '';
    $scope.searchMethod  = '';

    $scope.gen = { format: 'PDF', method: 'MANUAL' };

    $scope.resetGen = function () {
        $scope.gen = { format: 'PDF', method: 'MANUAL' };
    };

    $scope.generateReport = function () {
        // UI only — API 연동 시 구현
    };

    $scope.load = function () {
        $http.get(ctx + '/api/report/list')
            .then(function (res) { $scope.reportList = res.data; })
            .catch(function ()   { $scope.reportList = []; });
    };

    $scope.load();
}]);

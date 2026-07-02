'use strict';

qosApp.controller('StatisticsCtrl', ['$scope', '$http', function ($scope, $http) {

    $scope.stats           = { todayCollect: 0, todayDistribute: 0, monthCollect: 0, monthDistribute: 0 };
    $scope.collectStats    = [];
    $scope.distributeStats = [];

    $scope.load = function () {
        $http.get(ctx + '/api/statistics/summary')
            .then(function (res) { $scope.stats = res.data; })
            .catch(function ()   {});

        $http.get(ctx + '/api/statistics/collect')
            .then(function (res) { $scope.collectStats = res.data; })
            .catch(function ()   { $scope.collectStats = []; });

        $http.get(ctx + '/api/statistics/distribute')
            .then(function (res) { $scope.distributeStats = res.data; })
            .catch(function ()   { $scope.distributeStats = []; });
    };

    $scope.getStatusClass = function (result) {
        var map = { '성공': 'status-done', '실패': 'status-high', '처리중': 'status-processing' };
        return map[result] || '';
    };

    $scope.load();
}]);

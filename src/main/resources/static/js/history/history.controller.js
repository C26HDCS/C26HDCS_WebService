'use strict';

qosApp.controller('HistoryCtrl', ['$scope', '$http', function ($scope, $http) {

    $scope.historyList    = [];
    $scope.searchCategory = '';
    $scope.searchStartDate = '';
    $scope.searchEndDate   = '';

    $scope.load = function () {
        var params = {
            category:  $scope.searchCategory,
            startDate: $scope.searchStartDate,
            endDate:   $scope.searchEndDate
        };
        $http.get(ctx + '/api/history/list', { params: params })
            .then(function (res) { $scope.historyList = res.data; })
            .catch(function ()   { $scope.historyList = []; });
    };

    $scope.getResultClass = function (result) {
        var map = { '성공': 'status-done', '실패': 'status-high', '처리중': 'status-processing' };
        return map[result] || '';
    };

    $scope.load();
}]);

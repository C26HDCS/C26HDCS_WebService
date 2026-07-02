'use strict';

qosApp.controller('DistributionCtrl', ['$scope', '$http', function ($scope, $http) {

    $scope.distList = [];
    $scope.summary  = { total: 0, done: 0, pending: 0, failed: 0 };

    $scope.load = function () {
        $http.get(ctx + '/api/distribution/list')
            .then(function (res) {
                $scope.distList = res.data;
                $scope.summary.total   = res.data.length;
                $scope.summary.done    = res.data.filter(function (i) { return i.status === '완료'; }).length;
                $scope.summary.pending = res.data.filter(function (i) { return i.status === '대기'; }).length;
                $scope.summary.failed  = res.data.filter(function (i) { return i.status === '실패'; }).length;
            })
            .catch(function () { $scope.distList = []; });
    };

    $scope.getStatusClass = function (status) {
        var map = { '완료': 'status-done', '처리중': 'status-processing', '대기': 'status-wait', '실패': 'status-high' };
        return map[status] || '';
    };

    $scope.load();
}]);

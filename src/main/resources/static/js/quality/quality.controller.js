'use strict';

qosApp.controller('QualityCtrl', ['$scope', '$http', function ($scope, $http) {

    $scope.statusList   = [];
    $scope.searchKeyword = '';

    $scope.load = function () {
        $http.get('/qos/api/quality/status')
            .then(function (res) { $scope.statusList = res.data; })
            .catch(function ()   { $scope.statusList = []; });
    };

    $scope.getStatusClass = function (status) {
        var map = { '처리중': 'status-processing', '완료': 'status-done', '대기': 'status-wait' };
        return map[status] || '';
    };

    $scope.load();
}]);

'use strict';

qosApp.controller('AlgorithmCtrl', ['$scope', '$http', function ($scope, $http) {

    $scope.algorithmList = [];

    $scope.load = function () {
        $http.get('/qos/api/algorithm/list')
            .then(function (res) { $scope.algorithmList = res.data; })
            .catch(function ()   { $scope.algorithmList = []; });
    };

    $scope.toggleEnabled = function (item) {
        item.enabled = !item.enabled;
        // TODO: PUT /api/algorithm/{id} 호출로 서버 저장
    };

    $scope.load();
}]);

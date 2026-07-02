'use strict';

qosApp.controller('OutputCtrl', ['$scope', '$http', function ($scope, $http) {

    $scope.outputList    = [];
    $scope.searchKeyword = '';
    $scope.searchType    = '';

    $scope.load = function () {
        $http.get(ctx + '/api/output/list')
            .then(function (res) { $scope.outputList = res.data; })
            .catch(function ()   { $scope.outputList = []; });
    };

    $scope.getStatusClass = function (status) {
        var map = { '완료': 'status-done', '처리중': 'status-processing', '오류': 'status-high', '대기': 'status-wait' };
        return map[status] || '';
    };

    $scope.load();
}]);

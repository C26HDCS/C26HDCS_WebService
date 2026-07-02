'use strict';

qosApp.controller('SystemCtrl', ['$scope', '$http', function ($scope, $http) {

    $scope.sysInfo = {};

    $http.get('/qos/api/system/info')
        .then(function (res) { $scope.sysInfo = res.data; })
        .catch(function ()   { $scope.sysInfo = {}; });
}]);

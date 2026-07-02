'use strict';

qosApp.controller('IntroCtrl', ['$scope', '$http', function ($scope, $http) {

    $scope.intro       = { foundedYear: '-', employeeCount: 0, systemCount: 0, dataCount: 0 };
    $scope.info        = {};
    $scope.functions   = [];
    $scope.departments = [];

    $scope.load = function () {
        $http.get(ctx + '/api/intro/summary')
            .then(function (res) {
                $scope.intro       = res.data.intro;
                $scope.info        = res.data.info;
                $scope.functions   = res.data.functions;
                $scope.departments = res.data.departments;
            })
            .catch(function () {});
    };

    $scope.load();
}]);

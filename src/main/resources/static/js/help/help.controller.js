'use strict';

qosApp.controller('HelpCtrl', ['$scope', '$http', '$location', function ($scope, $http, $location) {
    var query = $location.search();
    var selectedCode = query.code;
    $scope.activeGuide = query.guide === 'dashboard' || query.guide === 'alarms' ? query.guide : 'codes';
    $scope.codeSearch = typeof selectedCode === 'string' ? selectedCode : '';
    $scope.errorCodes = [];

    $scope.loadErrorCodes = function () {
        $scope.loading = true;
        $scope.loadError = false;
        $http.get(ctx + '/api/log/error-codes').then(function (response) {
            $scope.errorCodes = response.data;
        }).catch(function () {
            $scope.loadError = true;
        }).finally(function () {
            $scope.loading = false;
        });
    };

    if ($scope.activeGuide === 'codes') $scope.loadErrorCodes();
}]);

'use strict';

qosApp.controller('MyPageCtrl', ['$scope', '$http', function ($scope, $http) {

    $scope.profile      = {};
    $scope.pw           = { current: '', newPw: '', confirm: '' };
    $scope.loginHistory = [];

    $scope.loadProfile = function () {
        $http.get(ctx + '/api/mypage/profile')
            .then(function (res) { $scope.profile = res.data; })
            .catch(function ()   { $scope.profile = {}; });
    };

    $scope.loadHistory = function () {
        $http.get(ctx + '/api/mypage/login-history')
            .then(function (res) { $scope.loginHistory = res.data; })
            .catch(function ()   { $scope.loginHistory = []; });
    };

    $scope.saveProfile = function () {
        $http.post(ctx + '/api/mypage/profile', $scope.profile)
            .then(function () { alert('저장되었습니다.'); })
            .catch(function () { alert('저장에 실패했습니다.'); });
    };

    $scope.changePassword = function () {
        if ($scope.pw.newPw !== $scope.pw.confirm) {
            alert('새 비밀번호가 일치하지 않습니다.');
            return;
        }
        $http.post(ctx + '/api/mypage/change-password', $scope.pw)
            .then(function () { alert('비밀번호가 변경되었습니다.'); $scope.pw = {}; })
            .catch(function () { alert('비밀번호 변경에 실패했습니다.'); });
    };

    $scope.loadProfile();
    $scope.loadHistory();
}]);

'use strict';

qosApp.controller('FtpCtrl', ['$scope', '$http', function ($scope, $http) {

    $scope.ftpList  = [];
    $scope.showForm = false;
    $scope.editMode = false;
    $scope.form     = {};

    $scope.load = function () {
        $http.get(ctx + '/api/ftp/list')
            .then(function (res) { $scope.ftpList = res.data; })
            .catch(function ()   { $scope.ftpList = []; });
    };

    $scope.edit = function (item) {
        $scope.form     = angular.copy(item);
        $scope.editMode = true;
        $scope.showForm = true;
    };

    $scope.save = function () {
        var url = $scope.editMode ? ctx + '/api/ftp/update' : ctx + '/api/ftp/create';
        $http.post(url, $scope.form)
            .then(function () { $scope.cancelForm(); $scope.load(); })
            .catch(function () { alert('저장에 실패했습니다.'); });
    };

    $scope.remove = function (item) {
        if (!confirm(item.name + ' 서버를 삭제하시겠습니까?')) { return; }
        $http.delete(ctx + '/api/ftp/' + item.id)
            .then(function () { $scope.load(); })
            .catch(function () { alert('삭제에 실패했습니다.'); });
    };

    $scope.testConnection = function (item) {
        $http.post(ctx + '/api/ftp/test', { id: item.id })
            .then(function (res) { alert(res.data.message || '연결 성공'); })
            .catch(function ()   { alert('연결에 실패했습니다.'); });
    };

    $scope.cancelForm = function () {
        $scope.showForm = false;
        $scope.editMode = false;
        $scope.form     = {};
    };

    $scope.load();
}]);

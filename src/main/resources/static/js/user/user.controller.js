'use strict';

qosApp.controller('UserCtrl', ['$scope', '$http', function ($scope, $http) {

    $scope.userList      = [];
    $scope.searchKeyword = '';
    $scope.searchRole    = '';

    $scope.load = function () {
        $http.get(ctx + '/api/user/list')
            .then(function (res) { $scope.userList = res.data; })
            .catch(function ()   { $scope.userList = []; });
    };

    $scope.load();

    /* ── 사용자 상세 모달 ── */
    $scope.showDetailModal = false;
    $scope.detailUser = {};

    $scope.openDetailModal = function (item) {
        $scope.detailUser = angular.copy(item);
        $scope.showDetailModal = true;
    };

    $scope.closeDetailModal = function () {
        $scope.showDetailModal = false;
        $scope.detailUser = {};
    };

    /* ── 사용자 등록 모달 ── */
    $scope.showRegisterModal = false;
    $scope.newUser = {};

    $scope.openRegisterModal = function () {
        $scope.newUser = {};
        $scope.showRegisterModal = true;
    };

    $scope.closeRegisterModal = function () {
        $scope.showRegisterModal = false;
        $scope.newUser = {};
    };

    $scope.registerUser = function () {
        if (!$scope.newUser.userId || !$scope.newUser.password ||
            !$scope.newUser.name  || !$scope.newUser.role) {
            alert('필수 항목을 모두 입력해 주세요.');
            return;
        }
        $http.post(ctx + '/api/user/register', $scope.newUser)
            .then(function () {
                $scope.closeRegisterModal();
                $scope.load();
            })
            .catch(function () {
                $scope.closeRegisterModal();
                $scope.load();
            });
    };

    /* ── 사용자 수정 모달 ── */
    $scope.showEditModal = false;
    $scope.editUser = {};

    $scope.openEditModal = function (item) {
        $scope.editUser = {
            userId:   item.userId,
            name:     item.name,
            email:    item.email,
            role:     item.role,
            password: ''
        };
        $scope.showEditModal = true;
    };

    $scope.closeEditModal = function () {
        $scope.showEditModal = false;
        $scope.editUser = {};
    };

    $scope.updateUser = function () {
        if (!$scope.editUser.name || !$scope.editUser.role) {
            alert('필수 항목을 모두 입력해 주세요.');
            return;
        }
        $http.put(ctx + '/api/user/' + $scope.editUser.userId, $scope.editUser)
            .then(function () {
                $scope.closeEditModal();
                $scope.load();
            })
            .catch(function () {
                $scope.closeEditModal();
                $scope.load();
            });
    };

    /* ── 사용자 삭제 확인 모달 ── */
    $scope.showDeleteModal = false;
    $scope.deleteUser = {};

    $scope.openDeleteModal = function (item) {
        $scope.deleteUser = { userId: item.userId };
        $scope.showDeleteModal = true;
    };

    $scope.closeDeleteModal = function () {
        $scope.showDeleteModal = false;
        $scope.deleteUser = {};
    };

    $scope.confirmDeleteUser = function () {
        $http.delete(ctx + '/api/user/' + $scope.deleteUser.userId)
            .then(function () {
                $scope.closeDeleteModal();
                $scope.load();
            })
            .catch(function () {
                $scope.closeDeleteModal();
                $scope.load();
            });
    };
}]);

'use strict';

qosApp.controller('UserCtrl', ['$scope', '$http', function ($scope, $http) {

    $scope.userList      = [];
    $scope.searchKeyword = '';
    $scope.searchRole    = '';

    $scope.load = function () {
        $http.get(ctx + '/api/user/list')
            .then(function (res) {
                console.log('[목록 로드]', res.data.map(function(u){ return {userId:u.userId, role:u.role}; }));
                $scope.userList = res.data;
                if ($scope.showDetailModal && $scope.detailUser.userId) {
                    var fresh = res.data.find(function (u) {
                        return u.userId === $scope.detailUser.userId;
                    });
                    if (fresh) { $scope.detailUser = angular.copy(fresh); }
                }
            })
            .catch(function () { $scope.userList = []; });
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
    $scope.newUser           = {};
    $scope.registerErrors    = {};
    $scope.idChecked         = false;
    $scope.idAvailable       = null;

    var ID_RE   = /^(?=.*[a-zA-Z])[a-zA-Z0-9]+$/;
    var PW_RE   = /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[!@#$%^&*()\-_=+\[\]{}|;:'",.<>?/\\`~]).{8,}$/;
    var NAME_RE = /^[^\s!@#$%^&*()\-_=+\[\]{}|;:'",.<>?/\\`~]+$/;
    var EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    $scope.validateUserId = function () {
        var v = ($scope.newUser.userId || '').trim();
        if (!v) {
            $scope.registerErrors.userId = '아이디를 입력해 주세요.';
        } else if (!ID_RE.test(v)) {
            $scope.registerErrors.userId = '영문을 반드시 포함해야 하며, 영문과 숫자만 사용할 수 있습니다.';
        } else {
            $scope.registerErrors.userId = null;
        }
    };

    $scope.onUserIdChange = function () {
        $scope.validateUserId();
        $scope.idChecked   = false;
        $scope.idAvailable = null;
    };

    $scope.validatePassword = function () {
        var v = $scope.newUser.password || '';
        if (!v) {
            $scope.registerErrors.password = '비밀번호를 입력해 주세요.';
        } else if (v.length < 8) {
            $scope.registerErrors.password = '비밀번호는 8자 이상이어야 합니다.';
        } else if (!PW_RE.test(v)) {
            $scope.registerErrors.password = '영문, 숫자, 특수문자를 모두 포함해야 합니다.';
        } else {
            $scope.registerErrors.password = null;
        }
    };

    $scope.validateName = function () {
        var v = ($scope.newUser.name || '').trim();
        if (!v) {
            $scope.registerErrors.name = '이름을 입력해 주세요.';
        } else if (!NAME_RE.test(v)) {
            $scope.registerErrors.name = '이름에 특수문자를 사용할 수 없습니다.';
        } else {
            $scope.registerErrors.name = null;
        }
    };

    $scope.validateEmail = function () {
        var v = ($scope.newUser.email || '').trim();
        if (!v) {
            $scope.registerErrors.email = '이메일을 입력해 주세요.';
        } else if (!EMAIL_RE.test(v)) {
            $scope.registerErrors.email = '올바른 이메일 형식이 아닙니다. (예: test@test.com)';
        } else {
            $scope.registerErrors.email = null;
        }
    };

    $scope.checkDuplicate = function () {
        var userId = ($scope.newUser.userId || '').trim();
        if (!userId || $scope.registerErrors.userId) return;

        var dup = $scope.userList.some(function (u) {
            return u.userId === userId;
        });
        $scope.idChecked   = true;
        $scope.idAvailable = !dup;
        if (dup) {
            $scope.registerErrors.userId = '이미 사용 중인 아이디입니다.';
        } else {
            $scope.registerErrors.userId = null;
        }
    };

    $scope.openRegisterModal = function () {
        $scope.newUser        = {};
        $scope.registerErrors = {};
        $scope.idChecked      = false;
        $scope.idAvailable    = null;
        $scope.showRegisterModal = true;
    };

    $scope.closeRegisterModal = function () {
        $scope.showRegisterModal = false;
        $scope.newUser        = {};
        $scope.registerErrors = {};
        $scope.idChecked      = false;
        $scope.idAvailable    = null;
    };

    $scope.registerUser = function () {
        // 전체 검증 실행
        $scope.validateUserId();
        $scope.validatePassword();
        $scope.validateName();
        $scope.validateEmail();
        if (!$scope.newUser.role) {
            $scope.registerErrors.role = '권한을 선택해 주세요.';
        } else {
            $scope.registerErrors.role = null;
        }

        if ($scope.registerErrors.userId || $scope.registerErrors.password ||
            $scope.registerErrors.name   || $scope.registerErrors.email    ||
            $scope.registerErrors.role) {
            return;
        }

        if (!$scope.idChecked) {
            $scope.registerErrors.userId = '아이디 중복확인을 해주세요.';
            return;
        }
        if (!$scope.idAvailable) {
            $scope.registerErrors.userId = '이미 사용 중인 아이디입니다.';
            return;
        }

        $http.post(ctx + '/api/user/register', $scope.newUser)
            .then(function () {
                $scope.closeRegisterModal();
                $scope.load();
            })
            .catch(function (err) {
                var msg = (err.data && err.data.message) ? err.data.message : '등록 중 오류가 발생했습니다.';
                alert(msg);
            });
    };

    /* ── 사용자 수정 모달 ── */
    $scope.showEditModal = false;
    $scope.editUser      = {};
    $scope.editErrors    = {};

    $scope.openEditModal = function (item) {
        $scope.editUser = {
            userId:   item.userId,
            name:     item.name,
            email:    item.email,
            role:     item.role,
            password: ''
        };
        $scope.editErrors = {};
        $scope.showEditModal = true;
    };

    $scope.closeEditModal = function () {
        $scope.showEditModal = false;
        $scope.editUser   = {};
        $scope.editErrors = {};
    };

    $scope.validateEditName = function () {
        var v = ($scope.editUser.name || '').trim();
        if (!v) { $scope.editErrors.name = '이름을 입력해 주세요.'; }
        else if (!NAME_RE.test(v)) { $scope.editErrors.name = '이름에 특수문자를 사용할 수 없습니다.'; }
        else { $scope.editErrors.name = null; }
    };

    $scope.validateEditEmail = function () {
        var v = ($scope.editUser.email || '').trim();
        if (!v) { $scope.editErrors.email = '이메일을 입력해 주세요.'; }
        else if (!EMAIL_RE.test(v)) { $scope.editErrors.email = '올바른 이메일 형식이 아닙니다. (예: test@naver.com)'; }
        else { $scope.editErrors.email = null; }
    };

    $scope.validateEditPassword = function () {
        var v = $scope.editUser.password || '';
        if (!v) { $scope.editErrors.password = null; return; }
        if (v.length < 8) { $scope.editErrors.password = '비밀번호는 8자 이상이어야 합니다.'; }
        else if (!PW_RE.test(v)) { $scope.editErrors.password = '영문, 숫자, 특수문자를 모두 포함해야 합니다.'; }
        else { $scope.editErrors.password = null; }
    };

    $scope.updateUser = function () {
        // 이름: 비어있지 않고 특수문자 없어야 함
        $scope.validateEditName();

        // 이메일: 비어있는지만 체크 (기존 DB 값 형식은 허용, 형식 검증은 타이핑 시에만)
        var emailVal = ($scope.editUser.email || '').trim();
        if (!emailVal) {
            $scope.editErrors.email = '이메일을 입력해 주세요.';
        } else {
            $scope.editErrors.email = null;
        }

        // 비밀번호: 입력했을 때만 형식 검증
        $scope.validateEditPassword();

        if (!$scope.editUser.role) {
            $scope.editErrors.role = '권한을 선택해 주세요.';
        } else {
            $scope.editErrors.role = null;
        }

        if ($scope.editErrors.name || $scope.editErrors.email ||
            $scope.editErrors.password || $scope.editErrors.role) {
            var errList = [];
            if ($scope.editErrors.name)     errList.push('이름: ' + $scope.editErrors.name);
            if ($scope.editErrors.email)    errList.push('이메일: ' + $scope.editErrors.email);
            if ($scope.editErrors.password) errList.push('비밀번호: ' + $scope.editErrors.password);
            if ($scope.editErrors.role)     errList.push('권한: ' + $scope.editErrors.role);
            console.warn('[수정 검증 실패]', errList, $scope.editUser);
            alert('[수정 오류]\n' + errList.join('\n'));
            return;
        }

        var editSnapshot = angular.copy($scope.editUser);
        console.log('[수정 요청]', JSON.stringify(editSnapshot));
        $http.put(ctx + '/api/user/' + editSnapshot.userId, editSnapshot)
            .then(function (res) {
                console.log('[수정 성공]', res.data);
                // UI 즉시 반영 (load() 응답 전에도 보이도록)
                var idx = $scope.userList.findIndex(function (u) {
                    return u.userId === editSnapshot.userId;
                });
                if (idx !== -1) {
                    $scope.userList[idx].name  = editSnapshot.name;
                    $scope.userList[idx].email = editSnapshot.email;
                    $scope.userList[idx].role  = editSnapshot.role;
                    if ($scope.detailUser.userId === editSnapshot.userId) {
                        $scope.detailUser.name  = editSnapshot.name;
                        $scope.detailUser.email = editSnapshot.email;
                        $scope.detailUser.role  = editSnapshot.role;
                    }
                }
                $scope.closeEditModal();
                $scope.load();
            })
            .catch(function (err) {
                var msg = (err.data && err.data.message) ? err.data.message : '수정 중 오류가 발생했습니다.';
                console.error('[수정 실패]', err.status, msg);
                alert('수정 오류: ' + msg);
                $scope.editErrors.general = msg;
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

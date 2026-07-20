'use strict';

qosApp.controller('MyPageCtrl', ['$scope', '$http', function ($scope, $http) {

    var PW_RE = /^(?=.*[a-zA-Z])(?=.*\d)(?=.*[!@#$%^&*()\-_=+\[\]{}|;:'",.<>?/\\`~]).{8,}$/;

    $scope.profile  = {};
    $scope.pw       = { current: '', newPw: '', confirm: '' };
    $scope.pwErrors = {};

    $scope.loadProfile = function () {
        $http.get(ctx + '/api/mypage/profile')
            .then(function (res) { $scope.profile = res.data; })
            .catch(function ()   { $scope.profile = {}; });
    };

    /* ── 새 비밀번호 유효성 검사 (user 수정 모달과 동일 규칙) ── */
    $scope.validateNewPassword = function () {
        var v = $scope.pw.newPw || '';
        if (!v) {
            $scope.pwErrors.newPw = null;
        } else if (v.length < 8) {
            $scope.pwErrors.newPw = '비밀번호는 8자 이상이어야 합니다.';
        } else if (!PW_RE.test(v)) {
            $scope.pwErrors.newPw = '영문, 숫자, 특수문자를 모두 포함해야 합니다.';
        } else {
            $scope.pwErrors.newPw = null;
        }
        // 확인란이 이미 입력된 경우 일치 여부 재검증
        if ($scope.pw.confirm) $scope.validateConfirmPassword();
    };

    $scope.validateConfirmPassword = function () {
        var v = $scope.pw.confirm || '';
        if (!v) {
            $scope.pwErrors.confirm = null;
        } else if ($scope.pw.newPw !== v) {
            $scope.pwErrors.confirm = '새 비밀번호가 일치하지 않습니다.';
        } else {
            $scope.pwErrors.confirm = null;
        }
    };

    /* ── 계정 정보 저장 ── */
    $scope.saveProfile = function () {
        var email = ($scope.profile.email || '').trim();
        if (!email) { alert('이메일을 입력해 주세요.'); return; }

        $http.post(ctx + '/api/mypage/profile', { email: email })
            .then(function (res) { alert(res.data.message || '저장되었습니다.'); })
            .catch(function (err) {
                var msg = err.data && err.data.message ? err.data.message : '저장에 실패했습니다.';
                alert(msg);
            });
    };

    /* ── 비밀번호 변경 ── */
    $scope.changePassword = function () {
        if (!$scope.pw.current) { alert('현재 비밀번호를 입력해 주세요.'); return; }
        if (!$scope.pw.newPw)   { alert('새 비밀번호를 입력해 주세요.'); return; }

        // submit 시점 전체 검증
        $scope.validateNewPassword();
        if (!$scope.pw.confirm) {
            $scope.pwErrors.confirm = '새 비밀번호 확인을 입력해 주세요.';
        } else {
            $scope.validateConfirmPassword();
        }

        if ($scope.pwErrors.newPw || $scope.pwErrors.confirm) return;

        $http.post(ctx + '/api/mypage/change-password', $scope.pw)
            .then(function (res) {
                alert(res.data.message || '비밀번호가 변경되었습니다.');
                $scope.pw       = { current: '', newPw: '', confirm: '' };
                $scope.pwErrors = {};
            })
            .catch(function (err) {
                var msg = err.data && err.data.message ? err.data.message : '비밀번호 변경에 실패했습니다.';
                alert(msg);
            });
    };

    $scope.loadProfile();
}]);

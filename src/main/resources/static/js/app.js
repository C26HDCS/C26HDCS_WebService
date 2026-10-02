'use strict';

var qosApp = angular.module('qosApp', ['ngRoute']);

/* ── ng-route 설정 ─────────────────────────────────────── */
qosApp.config(['$routeProvider', '$locationProvider', function ($routeProvider, $locationProvider) {

    $locationProvider.html5Mode({ enabled: true, requireBase: true });

    $routeProvider
        .when('/dashboard', {
            templateUrl: ctx + '/dashboard',
            controller:  'DashboardCtrl',
            title:       '대시보드'
        })
        .when('/output', {
            templateUrl: ctx + '/output',
            controller:  'OutputCtrl',
            title:       '산출물 조회'
        })
        .when('/distribution', {
            templateUrl: ctx + '/distribution',
            controller:  'DistributionCtrl',
            title:       '자료 배포 관리'
        })
        .when('/ftp', {
            templateUrl: ctx + '/ftp',
            controller:  'FtpCtrl',
            title:       'FTP 설정 관리'
        })
        .when('/user', {
            templateUrl: ctx + '/user',
            controller:  'UserCtrl',
            title:       '사용자 관리'
        })
        .when('/mypage', {
            templateUrl: ctx + '/mypage',
            controller:  'MyPageCtrl',
            title:       'MyPage'
        })
        .when('/log', {
            templateUrl: ctx + '/log',
            controller:  'LogCtrl',
            title:       '로그 및 이벤트 관리'
        })
        .when('/alarm', {
            templateUrl: ctx + '/alarm',
            controller:  'AlarmCtrl',
            title:       '알람 관리'
        })
        .when('/config', {
            templateUrl: ctx + '/config',
            controller:  'ConfigCtrl',
            title:       '환경설정 관리'
        })
        .when('/storage', {
            templateUrl: ctx + '/storage',
            controller:  'StorageCtrl',
            title:       '저장장치 관리'
        })
        .when('/report', {
            templateUrl: ctx + '/report',
            controller:  'ReportCtrl',
            title:       '리포트 관리'
        })
        .when('/history', {
            templateUrl: ctx + '/history',
            controller:  'HistoryCtrl',
            title:       '이력 관리'
        })
        .when('/statistics', {
            templateUrl: ctx + '/statistics',
            controller:  'StatisticsCtrl',
            title:       '통계'
        })
        .when('/intro', {
            templateUrl: ctx + '/intro',
            controller:  'IntroCtrl',
            title:       '기관 소개'
        })
        .otherwise({ redirectTo: '/dashboard' });
}]);

/* ── HTTP 공통 헤더 설정 ───────────────────────────────── */
qosApp.config(['$httpProvider', function ($httpProvider) {
    // Spring Security CSRF
    $httpProvider.defaults.xsrfCookieName = 'XSRF-TOKEN';
    $httpProvider.defaults.xsrfHeaderName = 'X-XSRF-TOKEN';
    // templateUrl 요청 시 서버가 partial view 반환하도록 식별
    $httpProvider.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest';

    // JWT 토큰을 매 요청마다 자동으로 헤더에 넣는 인터셉터
    $httpProvider.interceptors.push(['$q', '$window', function($q, $window) {
        return {
            // 서버로 요청을 보내기 직전에 가로채서 실행됨
            'request': function(config) {
                // 브라우저 저장소에서 로그인할 때 저장해둔 토큰을 꺼냅니다.
                // (주의: 로그인 화면에서 토큰을 저장할 때 쓴 이름과 동일해야 합니다. 예: 'jwtToken')
                var token = $window.localStorage.getItem('accessToken');
                
                if (token) {
                    // 토큰이 있으면 헤더에 달아줍니다.
                    config.headers['Authorization'] = 'Bearer ' + token;
                }
                return config;
            },
            // 서버에서 에러 응답이 왔을 때 실행됨
            'responseError': function(rejection) {
                // 401(인증 실패) 또는 403(권한 없음) 에러가 나면 콘솔에 찍고 로그인 화면으로 튕겨낼 수 있습니다.
                if (rejection.status === 401 || rejection.status === 403) {
                    console.error("인증 에러: 토큰이 없거나 만료되었습니다.");
                    // $window.location.href = '/login'; // 실제 운영 시엔 주석을 풀어서 로그인 화면으로 돌려보냅니다.
                }
                return $q.reject(rejection);
            }
        };
    }]);
}]);

/* ── 루트 컨트롤러 ────────────────────────────────────── */
qosApp.controller('AppCtrl', ['$scope', '$route', '$location', '$http', '$interval',
    function ($scope, $route, $location, $http, $interval) {

    // 페이지 타이틀 동기화
    $scope.$on('$routeChangeSuccess', function () {
        if ($route.current && $route.current.title) {
            $scope.pageTitle = $route.current.title;
        }
    });

    // 사이드바 활성 상태 판별
    $scope.isActive = function (path) {
        return $location.path() === path;
    };

    /* ── 헤더 알람 드롭다운 ── */
    var NOTIF_PREVIEW_SIZE = 10;

    $scope.unreadCount       = 0;
    $scope.notifList         = [];
    $scope.showNotifDropdown = false;

    $scope.loadUnreadCount = function () {
        $http.get(ctx + '/api/alarm/unread-count')
            .then(function (res) { $scope.unreadCount = res.data.count; })
            .catch(function () {});
    };

    $scope.loadRecentNotifications = function () {
        $http.get(ctx + '/api/alarm/list', { params: { limit: NOTIF_PREVIEW_SIZE } })
            .then(function (res) { $scope.notifList = res.data; })
            .catch(function () { $scope.notifList = []; });
    };

    $scope.toggleNotifDropdown = function ($event) {
        if ($event) $event.stopPropagation();
        $scope.showNotifDropdown = !$scope.showNotifDropdown;
        if ($scope.showNotifDropdown) $scope.loadRecentNotifications();
    };

    $scope.goToAlarmList = function () {
        $scope.showNotifDropdown = false;
        $location.path('/alarm');
    };

    $scope.formatNotifTime = function (occurredAt) {
        if (!occurredAt) return '-';
        return occurredAt.replace('T', ' ').substring(5, 16);
    };

    // 드롭다운 바깥 클릭 시 닫기
    document.addEventListener('click', function () {
        if ($scope.showNotifDropdown) $scope.$apply(function () { $scope.showNotifDropdown = false; });
    });

    $scope.loadUnreadCount();
    $interval($scope.loadUnreadCount, 3000);

    /* ── 알람 상세 팝업 (헤더 알림 클릭 / 알람 관리 화면 행 클릭에서 공통으로 재사용) ── */
    $scope.showDetailModal = false;
    $scope.detailAlarm     = {};

    $scope.getLevelClass = function (level) {
        var map = { 'HIGH': 'status-high', 'MEDIUM': 'status-medium', 'LOW': 'status-low' };
        return map[level] || '';
    };

    $scope.formatOccurredAt = function (occurredAt) {
        if (!occurredAt) return '-';
        return occurredAt.replace('T', ' ').substring(0, 16);
    };

    $scope.openDetailModal = function (item) {
        $scope.showNotifDropdown = false;
        $scope.detailAlarm = angular.copy(item);
        $scope.showDetailModal = true;

        if (!item.checked) {
            $http.post(ctx + '/api/alarm/' + item.id + '/check').then(function (res) {
                item.checked = true;
                $scope.detailAlarm.checked = true;
                $scope.unreadCount = res.data.unreadCount;
                // AppCtrl(부모)에서 AlarmCtrl(자식, ng-view 안)로 알려야 하므로 $broadcast 사용
                $scope.$broadcast('alarmChecked', item.id);
            });
        }
    };

    $scope.closeDetailModal = function () {
        $scope.showDetailModal = false;
        $scope.detailAlarm = {};
    };
}]);

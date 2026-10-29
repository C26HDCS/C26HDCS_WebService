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

qosApp.factory('AuthSession', ['$window', function ($window) {
    var redirecting = false;

    function token() {
        try { return $window.localStorage.getItem('accessToken'); }
        catch (error) { return null; }
    }

    // This local check only schedules sign-in; the server verifies JWT signatures.
    function valid() {
        try {
            var parts = (token() || '').split('.');
            if (parts.length !== 3) return false;
            var payload = parts[1].replace(/-/g, '+').replace(/_/g, '/');
            while (payload.length % 4) payload += '=';
            var claims = JSON.parse($window.atob(payload));
            return typeof claims.exp === 'number' && isFinite(claims.exp) && claims.exp * 1000 > Date.now();
        } catch (error) { return false; }
    }

    function expire() {
        if (redirecting) return;
        redirecting = true;
        try { $window.localStorage.removeItem('accessToken'); } catch (error) {}
        $window.location.replace(ctx + '/login');
    }

    function check() {
        if (redirecting) return false;
        if (valid()) return true;
        expire();
        return false;
    }

    function rejected(status) {
        if (status === 401 || (status === 403 && !valid())) expire();
    }

    return { token: token, check: check, rejected: rejected };
}]);

qosApp.run(['AuthSession', '$interval', '$window', function (session, $interval, $window) {
    session.check();
    $interval(session.check, 1000);
    $window.addEventListener('focus', session.check);
    $window.addEventListener('storage', function (event) {
        if (event.key === 'accessToken' || event.key === null) session.check();
    });
}]);

/* ── HTTP 공통 헤더 설정 ───────────────────────────────── */
qosApp.config(['$httpProvider', function ($httpProvider) {
    // Spring Security CSRF
    $httpProvider.defaults.xsrfCookieName = 'XSRF-TOKEN';
    $httpProvider.defaults.xsrfHeaderName = 'X-XSRF-TOKEN';
    // templateUrl 요청 시 서버가 partial view 반환하도록 식별
    $httpProvider.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest';

    // JWT 토큰을 매 요청마다 자동으로 헤더에 넣는 인터셉터
    $httpProvider.interceptors.push(['$q', 'AuthSession', function($q, session) {
        return {
            // 서버로 요청을 보내기 직전에 가로채서 실행됨
            'request': function(config) {
                if (!session.check()) return $q.reject({ status: 401, config: config });
                var token = session.token();
                
                if (token) {
                    // 토큰이 있으면 헤더에 달아줍니다.
                    config.headers['Authorization'] = 'Bearer ' + token;
                }
                return config;
            },
            // 서버에서 에러 응답이 왔을 때 실행됨
            'responseError': function(rejection) {
                session.rejected(rejection.status);
                return $q.reject(rejection);
            }
        };
    }]);
}]);

/* ── 루트 컨트롤러 ────────────────────────────────────── */
qosApp.controller('AppCtrl', ['$scope', '$route', '$location', '$http', '$interval', 'AuthSession', 'alarmSound',
    function ($scope, $route, $location, $http, $interval, session, alarmSound) {

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

    // 경고음: 읽지 않은 개수가 늘었을 때 1회만 울린다. (반복 없음, 끄기는 헤더 스피커 버튼)
    $scope.soundEnabled = alarmSound.isEnabled();
    $scope.soundBlocked = false;
    alarmSound.onBlockedChange = function (v) {
        $scope.$applyAsync(function () { $scope.soundBlocked = v; });
    };

    $scope.toggleAlarmSound = function () {
        // 켜져 있는데 브라우저가 막은 상태면, 이번 클릭으로 잠금을 풀고 시험음을 낸다.
        if ($scope.soundEnabled && $scope.soundBlocked) {
            if (alarmSound.unlock()) alarmSound.play();
            return;
        }
        $scope.soundEnabled = !$scope.soundEnabled;
        alarmSound.setEnabled($scope.soundEnabled);
        if ($scope.soundEnabled) { alarmSound.unlock(); alarmSound.play(); }
    };

    function setUnread(count) {
        $scope.unreadCount = count;
        alarmSound.observeUnread(count);
    }

    $scope.loadUnreadCount = function () {
        $http.get(ctx + '/api/alarm/unread-count')
            .then(function (res) { setUnread(res.data.count); })
            .catch(function () {});
    };

    // 알람 관리 화면(자식)에서 '모두 읽음 처리'를 했을 때 헤더 개수를 바로 맞춘다.
    $scope.$on('alarmAllChecked', function (event, unreadCount) {
        setUnread(unreadCount);
    });

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

    /* ── 알림 개수 실시간 수신 (SSE) ──
       연결을 열어 두고, 서버가 알림 개수가 바뀔 때만 보내 준다.
       인증 토큰이 요청 헤더로만 전달되므로(브라우저 기본 EventSource는 헤더를 못 붙임) fetch 로 스트림을 직접 읽는다. */
    var STREAM_URL      = ctx + '/api/alarm/stream';
    var RETRY_MIN_MS    = 3000;    // 재연결 최소 대기
    var RETRY_MAX_MS    = 30000;   // 재연결 최대 대기 (인증 실패 시에도 이 간격으로만 다시 시도)
    var IDLE_LIMIT_MS   = 60000;   // 서버는 20초마다 신호를 보낸다. 이 시간 동안 아무것도 안 오면 끊긴 것으로 보고 다시 연결
    var FALLBACK_POLL_MS = 15000;  // 스트림을 못 쓰는 오래된 브라우저용 대체 조회 주기

    var streamAbort   = null;
    var reconnectTimer = null;
    var idleTimer     = null;
    var retryDelay    = RETRY_MIN_MS;
    var streamClosed  = false;

    function readToken() {
        try { return window.localStorage.getItem('accessToken'); } catch (e) { return null; }
    }

    function supportsStream() {
        return !!(window.fetch && window.AbortController && window.TextDecoder && window.ReadableStream);
    }

    function resetIdleTimer(controller) {
        if (idleTimer) clearTimeout(idleTimer);
        idleTimer = setTimeout(function () { controller.abort(); }, IDLE_LIMIT_MS);
    }

    function scheduleReconnect(delay) {
        if (streamClosed) return;
        if (reconnectTimer) clearTimeout(reconnectTimer);
        reconnectTimer = setTimeout(connectStream, delay);
    }

    // 서버가 보낸 이벤트 한 건 처리
    function onStreamEvent(name, data) {
        if (name !== 'unread') return;
        var obj;
        try { obj = JSON.parse(data); } catch (e) { return; }
        if (typeof obj.count !== 'number') return;
        $scope.$applyAsync(function () {
            setUnread(obj.count);
            if ($scope.showNotifDropdown) $scope.loadRecentNotifications();
            $scope.$broadcast('alarmDataChanged');
        });
    }

    function connectStream() {
        if (streamClosed || !session.check()) return;

        var token = readToken();
        if (!token) {                       // 로그인 전이면 천천히 다시 확인
            scheduleReconnect(RETRY_MAX_MS);
            return;
        }

        var controller = new AbortController();
        streamAbort = controller;
        resetIdleTimer(controller);

        fetch(STREAM_URL, {
            headers: { 'Authorization': 'Bearer ' + token, 'Accept': 'text/event-stream' },
            cache: 'no-store',
            signal: controller.signal
        })
            .then(function (res) {
                if (!res.ok || !res.body) {
                    var err = new Error('stream ' + res.status);
                    err.status = res.status;
                    throw err;
                }
                var reader  = res.body.getReader();
                var decoder = new TextDecoder('utf-8');
                var buffer  = '';
                var evName  = 'message';
                var evData  = [];

                function handleLine(line) {
                    if (line === '') {                       // 빈 줄 = 이벤트 하나 끝
                        if (evData.length) {
                            retryDelay = RETRY_MIN_MS;       // 정상 수신 중이므로 재연결 대기 초기화
                            onStreamEvent(evName, evData.join('\n'));
                        }
                        evName = 'message';
                        evData = [];
                    } else if (line.charAt(0) === ':') {
                        // 주석(연결 유지 신호): 무시
                    } else if (line.indexOf('event:') === 0) {
                        evName = line.substring(6).replace(/^ /, '');
                    } else if (line.indexOf('data:') === 0) {
                        evData.push(line.substring(5).replace(/^ /, ''));
                    }
                }

                function pump() {
                    return reader.read().then(function (r) {
                        if (r.done) return;
                        resetIdleTimer(controller);
                        buffer += decoder.decode(r.value, { stream: true });
                        var lines = buffer.split(/\r?\n/);
                        buffer = lines.pop();                // 아직 덜 도착한 마지막 줄은 보관
                        lines.forEach(handleLine);
                        return pump();
                    });
                }
                return pump();
            })
            .then(function () {
                // 서버가 정상적으로 연결을 닫음(최대 유지 시간 도달 등): 바로 다시 연결
                scheduleReconnect(1000);
            })
            .catch(function (err) {
                if (streamClosed) return;
                if (err && (err.status === 401 || err.status === 403)) {
                    session.rejected(err.status);
                    scheduleReconnect(RETRY_MAX_MS);
                    return;
                }
                scheduleReconnect(retryDelay);
                retryDelay = Math.min(retryDelay * 2, RETRY_MAX_MS);
            });
    }

    function closeStream() {
        streamClosed = true;
        if (reconnectTimer) clearTimeout(reconnectTimer);
        if (idleTimer) clearTimeout(idleTimer);
        if (streamAbort) streamAbort.abort();
    }

    $scope.loadUnreadCount();
    if (supportsStream()) {
        connectStream();
        $scope.$on('$destroy', closeStream);
    } else {
        $interval($scope.loadUnreadCount, FALLBACK_POLL_MS);   // 대체: 15초마다 조회
    }

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
                setUnread(res.data.unreadCount);
                // AppCtrl(부모)에서 AlarmCtrl(자식, ng-view 안)로 알려야 하므로 $broadcast 사용
                $scope.$broadcast('alarmChecked', item.id);
            }).catch(function (err) {
                // 그 사이 삭제된 알람(404): 목록과 안 읽은 개수를 서버 기준으로 다시 맞춘다.
                if (err && err.status === 404) {
                    $scope.loadUnreadCount();
                    $scope.$broadcast('alarmDataChanged');
                }
            });
        }
    };

    $scope.closeDetailModal = function () {
        $scope.showDetailModal = false;
        $scope.detailAlarm = {};
    };
}]);

'use strict';

/*
 * 알람 경고음 (1차)
 *  - 읽지 않은 알람 개수가 늘어난 순간에만 경고음을 1회 낸다. (반복 없음)
 *  - 첫 값(페이지 로드 직후)은 기준값으로만 쓰고 소리를 내지 않는다.
 *  - 소리는 Web Audio 로 만든다(음원 파일 없음).
 *  - 브라우저 자동재생 정책 때문에 페이지에서 한 번이라도 클릭/키 입력이 있어야 소리가 난다.
 *    그 전에 울려야 했던 경우 blocked 상태가 되어 헤더 스피커 버튼에 표시한다.
 *  - 켜기/끄기는 브라우저(localStorage)별로 저장한다.
 */
qosApp.factory('alarmSound', ['$window', function ($window) {

    var STORAGE_KEY = 'hdcs.alarmSound.enabled';
    var service = { blocked: false, onBlockedChange: null };

    var ctx = null;
    var baseline = null;

    function readEnabled() {
        try { return $window.localStorage.getItem(STORAGE_KEY) !== 'off'; }
        catch (e) { return true; }
    }
    function writeEnabled(v) {
        try { $window.localStorage.setItem(STORAGE_KEY, v ? 'on' : 'off'); } catch (e) {}
    }
    var enabled = readEnabled();

    function setBlocked(v) {
        if (service.blocked === v) return;
        service.blocked = v;
        if (service.onBlockedChange) service.onBlockedChange(v);
    }

    function getContext() {
        if (ctx) return ctx;
        var AC = $window.AudioContext || $window.webkitAudioContext;
        if (!AC) return null;
        try { ctx = new AC(); } catch (e) { ctx = null; }
        return ctx;
    }

    // 사용자 동작(클릭/키 입력) 시 오디오를 미리 풀어 둔다.
    service.unlock = function () {
        var c = getContext();
        if (!c) return false;
        if (c.state === 'suspended' && c.resume) {
            try { c.resume(); } catch (e) {}
        }
        if (c.state === 'running') setBlocked(false);
        return c.state === 'running';
    };

    function tone(c, start, freq, dur) {
        var osc = c.createOscillator();
        var gain = c.createGain();
        osc.type = 'square';
        osc.frequency.value = freq;
        gain.gain.setValueAtTime(0.0001, start);
        gain.gain.exponentialRampToValueAtTime(0.18, start + 0.02);
        gain.gain.setValueAtTime(0.18, start + dur - 0.04);
        gain.gain.exponentialRampToValueAtTime(0.0001, start + dur);
        osc.connect(gain);
        gain.connect(c.destination);
        osc.start(start);
        osc.stop(start + dur + 0.02);
    }

    // 짧은 3회 삐 소리(약 0.85초). 재생을 시도했으면 true.
    service.play = function () {
        var c = getContext();
        if (!c) return false;
        if (c.state === 'suspended' && c.resume) {
            try { c.resume(); } catch (e) {}
        }
        if (c.state !== 'running') { setBlocked(true); return false; }
        setBlocked(false);
        var t = c.currentTime + 0.02;
        for (var i = 0; i < 3; i++) tone(c, t + i * 0.3, 880, 0.2);
        return true;
    };

    service.isEnabled = function () { return enabled; };
    service.setEnabled = function (v) { enabled = !!v; writeEnabled(enabled); };

    // 읽지 않은 개수가 갱신될 때마다 호출. 늘었을 때만 울린다.
    service.observeUnread = function (count) {
        if (typeof count !== 'number' || isNaN(count)) return false;
        var prev = baseline;
        baseline = count;
        if (prev === null || count <= prev) return false;
        if (!enabled) return false;
        return service.play();
    };

    // 첫 사용자 동작에서 오디오 잠금 해제
    ['click', 'keydown', 'touchstart'].forEach(function (evt) {
        $window.addEventListener(evt, function handler() {
            if (service.unlock()) $window.removeEventListener(evt, handler, true);
        }, true);
    });

    return service;
}]);

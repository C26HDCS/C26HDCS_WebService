'use strict';

/*
 * 토스트 알림 (화면 오른쪽 위에 잠깐 떴다 사라지는 안내)
 *  - toast.success(제목, 부가설명)   : 성공 알림 (약 3.5초)
 *  - toast.error(제목, 부가설명)     : 오류 알림 (약 5초)
 *  - 클릭하면 바로 닫힌다. 동시에 최대 4개까지 보이고, 넘치면 오래된 것부터 닫는다.
 *  - 글자는 textContent 로 넣어 서버 메시지에 HTML 이 들어 있어도 그대로 글자로만 보인다.
 */
qosApp.factory('toast', ['$window', function ($window) {

    var doc = $window.document;
    var MAX_VISIBLE = 4;
    var box = null;

    function container() {
        if (box && doc.body.contains(box)) return box;
        box = doc.createElement('div');
        box.className = 'toast-container';
        doc.body.appendChild(box);
        return box;
    }

    /*
     * 토스트 묶음의 세로 가운데를 "상단 헤더 아래 ~ 첫 번째 패널 위" 빈 공간의 정확한 중앙에 맞춘다.
     * (실제 화면 위치를 재서 계산하므로 화면 배율·제목 줄 높이가 달라도 같다.)
     * 측정할 수 없거나 스크롤로 빈 공간이 없으면 CSS 기본 위치를 그대로 쓴다.
     */
    function place(wrap) {
        var header = doc.querySelector('.top-header');
        var pageHeader = doc.querySelector('.page-header');
        var next = pageHeader && pageHeader.nextElementSibling;
        if (!header || !next) { wrap.style.top = ''; return; }
        var top = header.getBoundingClientRect().bottom;
        var bottom = next.getBoundingClientRect().top;
        if (bottom - top < 20) { wrap.style.top = ''; return; }
        wrap.style.top = ((top + bottom) / 2) + 'px';
    }

    function show(type, title, detail, ms) {
        var wrap = container();
        while (wrap.children.length >= MAX_VISIBLE) {
            wrap.removeChild(wrap.firstChild);
        }

        var el = doc.createElement('div');
        el.className = 'toast toast-' + type;
        el.setAttribute('role', type === 'error' ? 'alert' : 'status');

        var t = doc.createElement('div');
        t.className = 'toast-title';
        t.textContent = title;
        el.appendChild(t);

        if (detail) {
            var d = doc.createElement('div');
            d.className = 'toast-detail';
            d.textContent = detail;
            el.appendChild(d);
        }

        var closed = false;
        function close() {
            if (closed) return;
            closed = true;
            el.classList.add('toast-hide');
            $window.setTimeout(function () {
                if (el.parentNode) el.parentNode.removeChild(el);
            }, 200);
        }
        el.addEventListener('click', close);
        wrap.appendChild(el);
        place(wrap);
        $window.setTimeout(close, ms);
        return close;
    }

    return {
        success: function (title, detail) { return show('success', title, detail, 3500); },
        error:   function (title, detail) { return show('error',   title, detail, 5000); }
    };
}]);

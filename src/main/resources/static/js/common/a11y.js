'use strict';

/*
 * 키보드 접근성 공통 지시자
 *
 *  key-activate="표현식"
 *      목록 행처럼 클릭으로만 열리던 요소를 키보드로도 쓰게 한다.
 *      Tab 으로 포커스가 가고, 요소 자체에 포커스가 있을 때 Enter / Space 를 누르면 표현식을 실행한다.
 *      예) <tr ng-repeat="item in list" key-activate="openDetailModal(item)">
 *
 *  modal-a11y="열림 여부 표현식" modal-close="닫기 표현식"
 *      모달 창에 붙인다.
 *      - 열리면: 이전 포커스 위치를 기억하고, 창 안(입력칸이 있으면 첫 입력칸, 없으면 첫 버튼)으로 포커스를 옮긴다.
 *      - 열려 있는 동안: Tab / Shift+Tab 이 창 안에서만 돈다. Esc 로 닫는다.
 *      - 닫히면: 기억해 둔 위치로 포커스를 되돌린다.
 *      예) <div class="log-modal" ng-show="showDetailModal"
 *               modal-a11y="showDetailModal" modal-close="closeDetailModal()">
 */
qosApp.directive('keyActivate', function () {
    return {
        restrict: 'A',
        link: function (scope, el, attrs) {
            if (!el.attr('tabindex')) el.attr('tabindex', '0');
            if (!el.attr('role')) el.attr('role', 'button');
            if (!el.attr('aria-label')) el.attr('aria-label', '상세 정보 열기');

            el.on('keydown', function (e) {
                if (e.target !== el[0]) return;                 // 행 안의 버튼 등에서 누른 키는 무시
                if (e.which !== 13 && e.which !== 32) return;   // Enter, Space
                e.preventDefault();                              // Space 로 화면이 내려가지 않게
                scope.$apply(attrs.keyActivate);
            });
        }
    };
});

qosApp.directive('modalA11y', ['$timeout', function ($timeout) {
    var FOCUSABLE = 'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])';

    function visibleFocusables(root) {
        return Array.prototype.filter.call(root.querySelectorAll(FOCUSABLE), function (n) {
            return !n.disabled && n.type !== 'hidden' && n.offsetParent !== null;
        });
    }

    return {
        restrict: 'A',
        link: function (scope, el, attrs) {
            var node = el[0];
            var lastFocused = null;

            el.attr('role', 'dialog');
            el.attr('aria-modal', 'true');
            if (!el.attr('tabindex')) el.attr('tabindex', '-1');   // 포커스할 요소가 없어도 창 자체가 받도록

            scope.$watch(attrs.modalA11y, function (open, wasOpen) {
                if (open && !wasOpen) {
                    lastFocused = document.activeElement;
                    // ng-show 가 화면에 반영된 뒤에 포커스를 옮긴다.
                    $timeout(function () {
                        var field = visibleFocusables(node).filter(function (n) {
                            return /^(INPUT|SELECT|TEXTAREA)$/.test(n.tagName);
                        })[0];
                        var target = field || visibleFocusables(node)[0] || node;
                        target.focus();
                    }, 0);
                } else if (!open && wasOpen) {
                    var back = lastFocused;
                    lastFocused = null;
                    if (back && back.focus && document.body.contains(back)) back.focus();
                }
            });

            el.on('keydown', function (e) {
                if (e.which === 27) {                               // Esc
                    if (attrs.modalClose) {
                        e.stopPropagation();
                        scope.$apply(attrs.modalClose);
                    }
                    return;
                }
                if (e.which !== 9) return;                          // Tab 만 가둔다

                var items = visibleFocusables(node);
                if (!items.length) { e.preventDefault(); node.focus(); return; }

                var first = items[0], last = items[items.length - 1], active = document.activeElement;
                if (e.shiftKey && (active === first || active === node || !node.contains(active))) {
                    e.preventDefault(); last.focus();
                } else if (!e.shiftKey && (active === last || !node.contains(active))) {
                    e.preventDefault(); first.focus();
                }
            });
        }
    };
}]);

'use strict';

// 폼 제출 중복 방지
document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('form').forEach(function (form) {
        form.addEventListener('submit', function () {
            var btn = form.querySelector('button[type="submit"]');
            if (btn) {
                btn.disabled = true;
                setTimeout(function () { btn.disabled = false; }, 3000);
            }
        });
    });
});

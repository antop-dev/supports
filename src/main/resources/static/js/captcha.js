/**
 * 캡차 이미지를 클릭하면 새로 발급받는다.
 * CSP(script-src 'self')에서 인라인 onclick 은 실행되지 않으므로 별도 파일로 뺐다.
 */
(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-captcha-reload]').forEach(function (img) {
            img.addEventListener('click', function () {
                img.src = img.src.split('?')[0] + '?t=' + Date.now();
            });
        });
    });
})();

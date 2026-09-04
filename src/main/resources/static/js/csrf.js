(function (global) {
    'use strict';

    // CSRF 토큰은 레이아웃 head 의 meta 태그로 내려온다.
    // 일반 폼 전송은 폼 안의 히든 필드가 처리하고, fetch/XHR 는 여기서 헤더를 받아 쓴다.
    function meta(name) {
        var el = document.querySelector('meta[name="' + name + '"]');
        return el ? el.getAttribute('content') : null;
    }

    var headerName = meta('_csrf_header');
    var token = meta('_csrf');

    global.Csrf = {
        headerName: headerName,
        token: token,
        /** 주어진 헤더 객체에 CSRF 헤더를 얹어 새 객체로 돌려준다. */
        headers: function (extra) {
            var result = {};
            Object.keys(extra || {}).forEach(function (key) {
                result[key] = extra[key];
            });
            if (headerName && token) {
                result[headerName] = token;
            }
            return result;
        }
    };
})(window);

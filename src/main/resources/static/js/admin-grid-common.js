/** 관리자 Grid.js 화면 공통 유틸. window.AdminGrid 로 노출한다. */
(function (global) {
    'use strict';

    function pad(n) {
        return (n < 10 ? '0' : '') + n;
    }

    // 관리자 화면은 보는 사람의 시간대와 상관없이 항상 KST(UTC+9) 로 보여준다.
    // 한국은 서머타임이 없으므로 UTC 에 9시간을 더한 뒤 UTC 기준으로 읽으면 그대로 KST 다.
    var KST_OFFSET_MS = 9 * 60 * 60 * 1000;

    function toKst(ms) {
        return new Date(new Date(ms).getTime() + KST_OFFSET_MS);
    }

    /** epoch millis → yyyy.MM.dd HH:mm (KST) */
    function fmtDateTime(ms) {
        if (ms === null || ms === undefined) {
            return '-';
        }
        var d = toKst(ms);
        return d.getUTCFullYear() + '.' + pad(d.getUTCMonth() + 1) + '.' + pad(d.getUTCDate()) +
            ' ' + pad(d.getUTCHours()) + ':' + pad(d.getUTCMinutes());
    }

    /** 오늘 날짜(KST 기준) yyyy-MM-dd */
    function today() {
        var d = toKst(Date.now());
        return d.getUTCFullYear() + '-' + pad(d.getUTCMonth() + 1) + '-' + pad(d.getUTCDate());
    }

    function escapeHtml(s) {
        return String(s === null || s === undefined ? '' : s).replace(/[&<>"']/g, function (c) {
            return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
        });
    }

    var toastTimer = null;

    function toast(message, isError) {
        var el = document.getElementById('gjToast');
        if (!el) {
            el = document.createElement('div');
            el.id = 'gjToast';
            document.body.appendChild(el);
        }
        el.className = 'gj-toast' + (isError ? ' error' : '');
        el.textContent = message;
        // 브라우저가 전환 효과를 인식하도록 다음 프레임에 show 를 붙인다.
        requestAnimationFrame(function () {
            el.classList.add('show');
        });
        if (toastTimer) {
            clearTimeout(toastTimer);
        }
        toastTimer = setTimeout(function () {
            el.classList.remove('show');
        }, 2200);
    }

    /** JSON POST. 성공하면 body, 실패하면 message 를 담아 reject 한다. */
    function postJson(url, body) {
        return fetch(url, {
            method: 'POST',
            headers: window.Csrf.headers({ 'Content-Type': 'application/json' }),
            body: JSON.stringify(body || {})
        }).then(function (res) {
            return res.json().catch(function () { return {}; }).then(function (data) {
                if (!res.ok || data.ok === false) {
                    throw new Error(data.message || '처리하지 못했습니다.');
                }
                return data;
            });
        });
    }

    /** 검색 조건 객체 → 쿼리스트링(빈 값 제외) */
    function queryString(params) {
        var p = new URLSearchParams();
        Object.keys(params).forEach(function (k) {
            var v = params[k];
            if (v !== '' && v !== null && v !== undefined) {
                p.set(k, v);
            }
        });
        var q = p.toString();
        return q ? '?' + q : '';
    }

    /** Grid.js 서버 사이드 페이지네이션(고정 20건) 설정 */
    function serverPagination(limit) {
        return {
            limit: limit,
            buttonsCount: 5,
            server: {
                url: function (prev, page, pageLimit) {
                    return prev + (prev.indexOf('?') > -1 ? '&' : '?') + 'page=' + page + '&size=' + pageLimit;
                }
            }
        };
    }

    global.AdminGrid = {
        fmtDateTime: fmtDateTime,
        today: today,
        escapeHtml: escapeHtml,
        toast: toast,
        postJson: postJson,
        queryString: queryString,
        serverPagination: serverPagination,
        PAGE_SIZE: 20
    };
})(window);

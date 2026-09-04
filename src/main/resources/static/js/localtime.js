(function (global) {
    'use strict';

    // UTC(ISO) 값을 브라우저 시간대에 맞춰 고정 포맷으로 표시한다.
    // <time class="localtime" datetime="2026-07-26T10:15:30Z" data-format="datetime|date"></time>
    //   datetime(기본): yyyy.MM.dd HH:mm
    //   date          : yyyy.MM.dd
    function pad(n) {
        return n < 10 ? '0' + n : '' + n;
    }

    function format(el) {
        const iso = el.getAttribute('datetime');
        if (!iso) {
            return;
        }
        const d = new Date(iso);
        if (isNaN(d.getTime())) {
            return;
        }
        const kind = el.getAttribute('data-format') || 'datetime';
        const date = d.getFullYear() + '.' + pad(d.getMonth() + 1) + '.' + pad(d.getDate());
        el.textContent = kind === 'date'
            ? date
            : date + ' ' + pad(d.getHours()) + ':' + pad(d.getMinutes());
    }

    // 나중에 삽입된 화면(레이어 팝업 등)은 window.LocalTime.apply(root) 로 직접 적용한다.
    function apply(root) {
        (root || document).querySelectorAll('.localtime').forEach(format);
    }

    global.LocalTime = { apply: apply };

    document.addEventListener('DOMContentLoaded', function () {
        apply(document);
    });
})(window);

(function () {
    'use strict';

    // 클릭 시 텍스트를 클립보드에 복사한다.
    //  data-copy-text="복사할 문자열"      → 속성값을 복사
    //  data-copy-target="#선택자"          → 대상 요소의 텍스트를 복사
    //  data-copied="안내문구"              → 복사 후 토스트 문구(없으면 기본값)
    function toast(msg) {
        var t = document.createElement('div');
        t.className = 'toast';
        t.textContent = msg;
        document.body.appendChild(t);
        requestAnimationFrame(function () {
            t.classList.add('show');
        });
        setTimeout(function () {
            t.classList.remove('show');
            setTimeout(function () {
                t.remove();
            }, 300);
        }, 1500);
    }

    function copy(text, done) {
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(text).then(done, done);
            return;
        }
        var ta = document.createElement('textarea');
        ta.value = text;
        ta.style.position = 'fixed';
        ta.style.opacity = '0';
        document.body.appendChild(ta);
        ta.select();
        try {
            document.execCommand('copy');
        } catch (e) { /* ignore */ }
        ta.remove();
        done();
    }

    document.addEventListener('click', function (e) {
        var el = e.target.closest('[data-copy-text], [data-copy-target]');
        if (!el) {
            return;
        }
        var text;
        if (el.hasAttribute('data-copy-text')) {
            text = el.getAttribute('data-copy-text');
        } else {
            var target = document.querySelector(el.getAttribute('data-copy-target'));
            text = target ? target.textContent.trim() : '';
        }
        if (!text) {
            return;
        }
        copy(text, function () {
            toast(el.getAttribute('data-copied') || '복사되었습니다');
        });
    });
})();

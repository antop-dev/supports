(function () {
    'use strict';

    // 첨부 파일 중 이미지는 새 탭/다운로드 대신 페이지 위 팝업으로 바로 보여준다.
    // (PDF는 서버가 Content-Disposition: inline 으로 내려주므로 target=_blank 만으로 브라우저 뷰어에서 바로 열린다.)
    // 관리자 상세 팝업처럼 나중에 DOM에 끼워지는 링크도 잡아낼 수 있도록 document 에 위임한다.
    var overlay;
    var imgEl;
    var captionEl;

    function ensureOverlay() {
        if (overlay) {
            return;
        }
        overlay = document.createElement('div');
        overlay.className = 'file-preview-overlay';
        overlay.hidden = true;
        overlay.innerHTML =
            '<div class="file-preview-card">' +
            '<button type="button" class="file-preview-close" aria-label="close">&times;</button>' +
            '<img class="file-preview-img" alt="">' +
            '<div class="file-preview-caption"></div>' +
            '</div>';
        document.body.appendChild(overlay);
        imgEl = overlay.querySelector('.file-preview-img');
        captionEl = overlay.querySelector('.file-preview-caption');

        overlay.addEventListener('click', function (e) {
            if (e.target === overlay || e.target.closest('.file-preview-close')) {
                closePreview();
            }
        });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && !overlay.hidden) {
                closePreview();
            }
        });
    }

    function openPreview(src, name) {
        ensureOverlay();
        imgEl.src = src;
        imgEl.alt = name || '';
        captionEl.textContent = name || '';
        overlay.hidden = false;
    }

    function closePreview() {
        overlay.hidden = true;
        imgEl.src = '';
    }

    document.addEventListener('click', function (e) {
        var link = e.target.closest('[data-preview="image"]');
        if (!link) {
            return;
        }
        e.preventDefault();
        openPreview(link.href, link.textContent.trim());
    });
})();

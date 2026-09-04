(function (global) {
    'use strict';

    // [data-editor] 요소를 Quill 에디터로 초기화하고, 폼 제출 시 HTML을 hidden input에 반영한다.
    // data-editor="필드명", data-target="#hiddenInputSelector"
    // 나중에 삽입된 화면(레이어 팝업 등)은 window.QuillInit.init(root) 로 직접 초기화한다.
    const toolbar = [
        ['bold', 'italic', 'underline', 'strike'],
        [{ align: [] }],
        ['blockquote', 'code-block', 'link'],
        ['clean']
    ];

    function init(root) {
        if (typeof Quill === 'undefined') {
            return;
        }
        const scope = root || document;
        // 이미지 첨부는 별도 파일 업로드로만 받는다. 붙여넣기로 들어오는 이미지는 두 경로 모두 막는다.
        const Delta = Quill.import('delta');

        scope.querySelectorAll('[data-editor]').forEach(function (el) {
            // 같은 요소를 두 번 초기화하지 않는다.
            if (el.dataset.editorReady === 'true') {
                return;
            }
            el.dataset.editorReady = 'true';

            const targetSel = el.getAttribute('data-target');
            const target = targetSel ? (scope.querySelector(targetSel) || document.querySelector(targetSel)) : null;

            const quill = new Quill(el, {
                theme: 'snow',
                modules: {
                    toolbar: toolbar,
                    // 스크린샷/이미지 파일 붙여넣기·드래그(base64 삽입)를 막는다.
                    // mimetypes: [] 는 Quill 기본값과 배열 인덱스 기준으로 병합되어 무시되므로,
                    // handler 자체를 아무 것도 하지 않도록 덮어써서 막는다.
                    uploader: { handler: function () {} }
                }
            });
            // 다른 페이지에서 복사한 <img> 태그 붙여넣기를 막는다.
            quill.clipboard.addMatcher('img', function () {
                return new Delta();
            });

            // 초기값 주입
            if (target && target.value) {
                quill.clipboard.dangerouslyPasteHTML(target.value);
            }

            const form = el.closest('form');
            if (form && target) {
                form.addEventListener('submit', function () {
                    const html = quill.getText().trim().length === 0 ? '' : quill.root.innerHTML;
                    target.value = html;
                });
            }
        });
    }

    global.QuillInit = { init: init };

    document.addEventListener('DOMContentLoaded', function () {
        init(document);
    });
})(window);

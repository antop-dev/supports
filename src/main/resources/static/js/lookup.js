(function () {
    'use strict';

    // 접수번호 검색을 AJAX로 보낸다.
    //  - 찾음: 서버가 준 redirect(상세 페이지)로 이동
    //  - 못 찾음(400): 입력창 아래에 오류 표시
    //  - 검색 버튼: 누르는 즉시 인디케이터를 돌리고 비활성화해서 중복 요청을 막는다(쓰로틀링).
    document.addEventListener('DOMContentLoaded', function () {
        var form = document.getElementById('lookupForm');
        if (!form) {
            return;
        }

        var field = form.querySelector('.field[data-field="receiptNo"]');
        var errEl = field ? field.querySelector('[data-error]') : null;
        var submitBtn = form.querySelector('button[type="submit"]');
        var submitting = false; // 이중 클릭/중복 submit 이벤트를 막는 잠금

        function clearError() {
            if (field) {
                field.classList.remove('has-error');
            }
            if (errEl) {
                errEl.textContent = '';
            }
        }

        function showError(message) {
            if (field) {
                field.classList.add('has-error');
            }
            if (errEl) {
                errEl.textContent = message;
            }
        }

        function setLoading(loading) {
            if (!submitBtn) {
                return;
            }
            submitBtn.disabled = loading;
            submitBtn.classList.toggle('is-loading', loading);
        }

        // 검색 성공 시 이동한 뒤 뒤로가기로 돌아오면, 페이지가 bfcache에서 그대로 복원되어
        // 버튼이 "검색 중" 상태로 멈춰 있을 수 있다. 복원될 때마다 원래대로 되돌린다.
        window.addEventListener('pageshow', function (e) {
            if (e.persisted) {
                submitting = false;
                setLoading(false);
            }
        });

        form.addEventListener('submit', function (e) {
            e.preventDefault();
            if (submitting) {
                return;
            }
            submitting = true;
            clearError();
            setLoading(true);

            fetch(form.action, {
                method: 'POST',
                body: new FormData(form),
                headers: { 'X-Requested-With': 'XMLHttpRequest' }
            }).then(function (res) {
                return res.json().then(function (data) {
                    return { ok: res.ok, data: data };
                });
            }).then(function (r) {
                if (r.ok && r.data.redirect) {
                    window.location.href = r.data.redirect;
                    return;
                }
                var message = (r.data.fieldErrors && r.data.fieldErrors.receiptNo) || '오류가 발생했습니다.';
                showError(message);
                submitting = false;
                setLoading(false);
            }).catch(function () {
                showError('오류가 발생했습니다. 잠시 후 다시 시도해 주세요.');
                submitting = false;
                setLoading(false);
            });
        });
    });
})();

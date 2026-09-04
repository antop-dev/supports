/**
 * 관리자 비밀번호 변경 팝업.
 * 상단 "비밀번호 변경"에서 열고, 변경에 성공하면 세션이 끊기므로 로그인 화면으로 보낸다.
 */
(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        var btn = document.getElementById('passwordBtn');
        var modal = document.getElementById('passwordModal');
        if (!btn || !modal) {
            return;
        }

        var url = modal.getAttribute('data-url');
        var loginUrl = modal.getAttribute('data-login-url');
        var form = document.getElementById('passwordForm');
        var current = document.getElementById('pw-current');
        var next = document.getElementById('pw-new');
        var confirm = document.getElementById('pw-confirm');
        var errorEl = document.getElementById('pw-error');
        var saveBtn = document.getElementById('pw-save');

        function showError(message) {
            errorEl.textContent = message;
            errorEl.classList.remove('hidden');
        }

        function openModal() {
            form.reset();
            errorEl.classList.add('hidden');
            modal.classList.remove('hidden');
            current.focus();
        }

        function closeModal() {
            modal.classList.add('hidden');
        }

        btn.addEventListener('click', openModal);
        modal.querySelectorAll('[data-modal-close]').forEach(function (el) {
            el.addEventListener('click', closeModal);
        });
        modal.addEventListener('click', function (e) {
            if (e.target === modal) {
                closeModal();
            }
        });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && !modal.classList.contains('hidden')) {
                closeModal();
            }
        });

        form.addEventListener('submit', function (e) {
            e.preventDefault();
            errorEl.classList.add('hidden');

            if (!current.value || !next.value || !confirm.value) {
                showError('모든 항목을 입력하세요.');
                return;
            }
            if (next.value !== confirm.value) {
                showError('새 비밀번호가 서로 일치하지 않습니다.');
                return;
            }

            saveBtn.disabled = true;
            fetch(url, {
                method: 'POST',
                headers: window.Csrf.headers({ 'Content-Type': 'application/json' }),
                body: JSON.stringify({
                    currentPassword: current.value,
                    newPassword: next.value,
                    confirmPassword: confirm.value
                })
            }).then(function (res) {
                return res.json().catch(function () { return {}; }).then(function (data) {
                    if (!res.ok || data.ok === false) {
                        throw new Error(data.message || '비밀번호를 변경하지 못했습니다.');
                    }
                    return data;
                });
            }).then(function () {
                // 세션은 서버에서 이미 끊었다. 로그인 화면으로 이동한다.
                location.href = loginUrl;
            }).catch(function (err) {
                saveBtn.disabled = false;
                showError(err.message);
            });
        });
    });
})();

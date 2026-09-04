/**
 * 관리자 로그인 화면.
 *
 * 로그인은 페이지 이동 없이 AJAX 로 처리한다. 요청이 나가 있는 동안에는
 * 버튼을 비활성 + 스피너로 바꿔 두 번 이상 시도할 수 없게 한다.
 * 캡차는 서버에서 한 번 검증하면 세션에서 지워지므로, 실패하면 이미지를 새로 받는다.
 */
(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        var form = document.getElementById('loginForm');
        if (!form) {
            return;
        }

        var url = form.getAttribute('data-url');
        var successUrl = form.getAttribute('data-success-url');
        var errorText = form.getAttribute('data-error-text');
        var username = document.getElementById('login-username');
        var password = document.getElementById('login-password');
        var captcha = document.getElementById('login-captcha');
        var captchaImg = document.getElementById('captchaImg');
        var globalError = document.getElementById('login-error');
        var submitBtn = document.getElementById('login-submit');
        var submitIcon = document.getElementById('login-submit-icon');
        var submitText = document.getElementById('login-submit-text');
        var fieldErrors = form.querySelectorAll('[data-error-for]');
        // 요청이 나가 있는 동안 다시 제출되는 것을 막는다(엔터 연타 등 버튼 밖의 경로까지).
        var submitting = false;

        // CSP 에서 인라인 onclick 을 피하려고 클릭 새로고침도 여기서 붙인다.
        function reloadCaptcha() {
            if (captchaImg) {
                captchaImg.src = captchaImg.src.split('?')[0] + '?t=' + Date.now();
            }
        }

        if (captchaImg) {
            captchaImg.addEventListener('click', reloadCaptcha);
        }

        function clearErrors() {
            globalError.textContent = '';
            globalError.classList.add('hidden');
            fieldErrors.forEach(function (el) {
                el.textContent = '';
                el.classList.add('hidden');
            });
        }

        function showErrors(message, errors) {
            if (message) {
                globalError.textContent = message;
                globalError.classList.remove('hidden');
            }
            fieldErrors.forEach(function (el) {
                var text = errors ? errors[el.getAttribute('data-error-for')] : null;
                if (text) {
                    el.textContent = text;
                    el.classList.remove('hidden');
                }
            });
        }

        // 캡차는 매번 다시 입력해야 하므로, 아이디/비밀번호에 문제가 없으면 캡차로 커서를 보낸다.
        function focusFirstProblem(errors) {
            if (errors && errors.username) {
                username.focus();
            } else if (errors && errors.password) {
                password.focus();
            } else {
                captcha.focus();
            }
        }

        function setBusy(busy) {
            submitting = busy;
            submitBtn.disabled = busy;
            form.setAttribute('aria-busy', busy ? 'true' : 'false');
            submitIcon.className = busy ? 'fa-solid fa-spinner fa-spin' : 'fa-solid fa-right-to-bracket';
            submitText.textContent = submitBtn.getAttribute(busy ? 'data-busy-text' : 'data-idle-text');
        }

        form.addEventListener('submit', function (e) {
            e.preventDefault();
            if (submitting) {
                return;
            }
            clearErrors();
            setBusy(true);

            fetch(url, {
                method: 'POST',
                headers: window.Csrf.headers({ 'Content-Type': 'application/json' }),
                body: JSON.stringify({
                    username: username.value,
                    password: password.value,
                    captcha: captcha.value
                })
            }).then(function (res) {
                return res.json().catch(function () { return {}; }).then(function (data) {
                    if (!res.ok || data.ok === false) {
                        throw data.ok === false ? data : { message: errorText };
                    }
                    return data;
                });
            }).then(function (data) {
                // 성공하면 그대로 화면을 넘긴다. 이동하는 동안 버튼은 스피너로 둔다.
                // 로그인 전에 들어오려던 화면이 있으면(예: 슬랙 알림 링크) 그쪽으로 보낸다.
                location.href = (data && data.redirect) || successUrl;
            }).catch(function (err) {
                // 서버가 내려준 실패 응답이 아니면(통신 오류 등) 공통 문구로 알린다.
                var failure = err && err.ok === false ? err : { message: errorText, errors: {} };
                setBusy(false);
                // 검증에 쓴 캡차는 이미 소모됐다. 새 이미지를 받고 입력값을 비운다.
                reloadCaptcha();
                captcha.value = '';
                showErrors(failure.message, failure.errors);
                focusFirstProblem(failure.errors);
            });
        });
    });
})();

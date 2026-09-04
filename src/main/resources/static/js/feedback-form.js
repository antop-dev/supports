(function () {
    'use strict';

    // 의견 등록 폼을 AJAX로 전송한다.
    //  - 성공: 서버가 준 redirect(완료 페이지)로 이동
    //  - 검증 실패(400): 필드별/전역 오류를 화면에 표시하고, 1회용 캡차를 새로 발급
    //  - 제출 버튼: 누르는 즉시 인디케이터를 돌리고 비활성화해서 중복 제출을 막는다(쓰로틀링).
    //    첨부 파일이 아직 청크 업로드 중이면(대용량 파일은 오래 걸릴 수 있다) 끝날 때까지 기다렸다가 보낸다.
    document.addEventListener('DOMContentLoaded', function () {
        var form = document.getElementById('feedbackForm');
        if (!form) {
            return;
        }
        // quill-init 이 submit 시 에디터 내용을 hidden input 에 동기화한다.
        // 이 스크립트는 scripts 프래그먼트(quill-init 포함) 다음에 로드되므로,
        // submit 리스너 등록 순서상 동기화가 먼저 실행된 뒤 FormData 를 읽는다.

        var globalBox = document.querySelector('[data-global-errors]');
        var submitBtn = form.querySelector('button[type="submit"]');
        var submitting = false; // 이중 클릭/중복 submit 이벤트를 막는 잠금
        var UPLOAD_WAIT_TIMEOUT_MS = 30 * 60 * 1000; // 대용량 파일도 넉넉히 기다린다
        var UPLOAD_POLL_INTERVAL_MS = 300;

        function clearErrors() {
            form.querySelectorAll('.field.has-error').forEach(function (f) {
                f.classList.remove('has-error');
            });
            form.querySelectorAll('[data-error]').forEach(function (e) {
                e.textContent = '';
            });
            if (globalBox) {
                globalBox.textContent = '';
                globalBox.hidden = true;
            }
        }

        function showFieldErrors(fieldErrors) {
            Object.keys(fieldErrors || {}).forEach(function (name) {
                var field = form.querySelector('.field[data-field="' + name + '"]');
                if (!field) {
                    return;
                }
                field.classList.add('has-error');
                var errEl = field.querySelector('[data-error]');
                if (errEl) {
                    errEl.textContent = fieldErrors[name];
                }
            });
        }

        function showGlobalErrors(globalErrors) {
            if (!globalBox || !globalErrors || globalErrors.length === 0) {
                return;
            }
            globalBox.textContent = '';
            globalErrors.forEach(function (m) {
                var d = document.createElement('div');
                d.textContent = m;
                globalBox.appendChild(d);
            });
            globalBox.hidden = false;
        }

        function reloadCaptcha() {
            var img = document.getElementById('captchaImg');
            if (img) {
                img.src = img.src.split('?')[0] + '?t=' + Date.now();
            }
        }

        function setLoading(loading) {
            if (!submitBtn) {
                return;
            }
            submitBtn.disabled = loading;
            submitBtn.classList.toggle('is-loading', loading);
        }

        // 등록 성공 후 완료 페이지로 이동했다가 뒤로가기로 돌아오면, 페이지가 bfcache에서
        // 그대로 복원되어 버튼이 "등록 중" 상태로 멈춰 있을 수 있다. 복원될 때마다 되돌린다.
        window.addEventListener('pageshow', function (e) {
            if (e.persisted) {
                submitting = false;
                setLoading(false);
            }
        });

        /** 첨부 파일 청크 업로드가 끝날 때까지 기다린다. 실패/타임아웃이면 reject. */
        function waitForUploads() {
            return new Promise(function (resolve, reject) {
                if (!window.FilePondInit) {
                    resolve();
                    return;
                }
                var start = Date.now();
                (function poll() {
                    if (window.FilePondInit.hasError()) {
                        reject(new Error('첨부 파일 업로드에 실패했습니다. 파일을 확인해주세요.'));
                        return;
                    }
                    if (!window.FilePondInit.isBusy()) {
                        resolve();
                        return;
                    }
                    if (Date.now() - start > UPLOAD_WAIT_TIMEOUT_MS) {
                        reject(new Error('파일 업로드 시간이 너무 오래 걸립니다. 잠시 후 다시 시도해주세요.'));
                        return;
                    }
                    setTimeout(poll, UPLOAD_POLL_INTERVAL_MS);
                })();
            });
        }

        form.addEventListener('submit', function (e) {
            e.preventDefault();
            if (submitting) {
                return;
            }
            submitting = true;
            clearErrors();
            setLoading(true);

            waitForUploads()
                .then(function () {
                    return fetch(form.action, {
                        method: 'POST',
                        body: new FormData(form),
                        headers: { 'X-Requested-With': 'XMLHttpRequest' }
                    });
                })
                .then(function (res) {
                    return res.json().then(function (data) {
                        return { ok: res.ok, data: data };
                    });
                })
                .then(function (r) {
                    if (r.ok && r.data.redirect) {
                        window.location.href = r.data.redirect;
                        return;
                    }
                    showFieldErrors(r.data.fieldErrors);
                    showGlobalErrors(r.data.globalErrors);
                    reloadCaptcha(); // 캡차는 1회용이므로 실패 시 새로 발급
                    submitting = false;
                    setLoading(false);
                    var firstErr = form.querySelector('.field.has-error');
                    if (firstErr) {
                        firstErr.scrollIntoView({ behavior: 'smooth', block: 'center' });
                    }
                })
                .catch(function (err) {
                    reloadCaptcha();
                    submitting = false;
                    setLoading(false);
                    alert(err && err.message ? err.message : '오류가 발생했습니다. 잠시 후 다시 시도해 주세요.');
                });
        });
    });
})();

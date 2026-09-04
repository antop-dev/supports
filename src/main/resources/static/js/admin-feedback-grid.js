/** 관리자 의견 목록 그리드. 한 페이지 20건 고정. */
(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        var mount = document.getElementById('grid');
        if (!mount || typeof gridjs === 'undefined') {
            return;
        }

        var G = window.AdminGrid;
        var dataUrl = mount.getAttribute('data-url');       // /ctx/admin/feedbacks/data
        var detailBase = mount.getAttribute('data-detail'); // /ctx/admin/feedbacks

        var state = { receiptNo: '', title: '', email: '', status: '' };

        /** 태그 라벨 하나. title 을 주면 마우스를 올렸을 때 툴팁이 뜬다. */
        function tag(cls, icon, text, tooltip) {
            return '<span class="gj-badge ' + cls + '"' +
                (tooltip ? ' title="' + G.escapeHtml(tooltip) + '"' : '') + '>' +
                '<i class="' + icon + '"></i><span>' + G.escapeHtml(text) + '</span></span>';
        }

        /** 이메일 라벨. 아이콘에만 mailto: 링크를 건다(주소 글자를 눌러도 메일이 열리지 않게). */
        function emailTag(email) {
            var safe = G.escapeHtml(email);
            return '<span class="gj-badge gj-email" title="' + safe + '">' +
                '<a class="gj-mailto" href="mailto:' + G.escapeHtml(encodeURI(email)) + '" title="' + safe + '">' +
                '<i class="fa-regular fa-envelope"></i></a>' +
                '<span>' + safe + '</span></span>';
        }

        function statusTag(status, processedAt) {
            // 완료 건은 처리일시를 툴팁으로 보여준다.
            return status === 'DONE'
                ? tag('gj-done', 'fa-solid fa-check', '완료', '처리일시 ' + G.fmtDateTime(processedAt))
                : tag('gj-recv', 'fa-regular fa-clock', '접수', '');
        }

        function serverConfig() {
            return {
                url: dataUrl + G.queryString(state),
                then: function (data) {
                    return data.data.map(function (r) {
                        return [r.receiptNo, r.id, r.title, r.createdAt, r.secret, r.email, r.status, r.processedAt];
                    });
                },
                total: function (data) {
                    return data.total;
                }
            };
        }

        // 접수번호/보기/등록일시는 고정 폭, 의견 열이 남은 폭을 모두 차지한다.
        var columns = [
            { name: '접수번호', width: '180px' },
            {
                name: '보기',
                width: '90px',
                sort: false,
                // 상세는 이 버튼으로만 연다.
                formatter: function (cell) {
                    return gridjs.html(
                        '<button type="button" class="gj-view-btn" data-view="' + G.escapeHtml(cell) + '">' +
                        '<i class="fa-regular fa-eye"></i> 보기</button>'
                    );
                }
            },
            {
                name: '의견',
                // 첫 줄은 제목, 둘째 줄은 처리상태/비밀글/이메일 태그.
                formatter: function (cell, row) {
                    var secret = row.cells[4].data;
                    var email = row.cells[5].data;
                    var tags = statusTag(row.cells[6].data, row.cells[7].data);
                    if (secret) {
                        tags += tag('gj-lock', 'fa-solid fa-lock', '비밀글', '');
                    }
                    if (email) {
                        tags += emailTag(email);
                    }
                    return gridjs.html(
                        '<div class="gj-title" title="' + G.escapeHtml(cell) + '">' + G.escapeHtml(cell) + '</div>' +
                        '<div class="gj-tags">' + tags + '</div>'
                    );
                }
            },
            {
                name: '등록일시',
                width: '130px',
                formatter: function (cell) {
                    return gridjs.html('<span class="gj-date">' + G.escapeHtml(G.fmtDateTime(cell)) + '</span>');
                }
            },
            { id: 'secret', name: 'secret', hidden: true },
            { id: 'email', name: 'email', hidden: true },
            { id: 'status', name: 'status', hidden: true },
            { id: 'processedAt', name: 'processedAt', hidden: true }
        ];

        var grid = new gridjs.Grid({
            columns: columns,
            // 지정한 열 폭을 그대로 쓴다(autoWidth 는 내용에 맞춰 폭을 다시 계산한다).
            autoWidth: false,
            server: serverConfig(),
            pagination: G.serverPagination(G.PAGE_SIZE),
            fixedHeader: true,
            language: window.gridjsLocaleKr
        }).render(mount);

        function applySearch() {
            state.receiptNo = document.getElementById('q-receiptNo').value.trim();
            state.title = document.getElementById('q-title').value.trim();
            state.email = document.getElementById('q-email').value.trim();
            state.status = document.getElementById('q-status').value;
            grid.updateConfig({ server: serverConfig(), pagination: G.serverPagination(G.PAGE_SIZE) }).forceRender();
        }

        document.getElementById('searchForm').addEventListener('submit', function (e) {
            e.preventDefault();
            applySearch();
        });
        // 처리상태는 고른 즉시 검색하지 않는다. 검색 버튼을 눌러야 반영된다.
        document.getElementById('resetBtn').addEventListener('click', function () {
            // reset 이 값에 반영된 뒤에 검색한다.
            setTimeout(applySearch, 0);
        });

        // ---------- 상세 레이어 팝업 ----------

        var modal = document.getElementById('detailModal');
        var panel = document.getElementById('detailPanel');
        var openRow = null; // 팝업을 연 목록 행(상태가 바뀌면 이 행만 갱신한다)

        function isOpen() {
            return !modal.classList.contains('hidden');
        }

        function closeModal() {
            if (!isOpen()) {
                return;
            }
            modal.classList.add('hidden');
            panel.innerHTML = ''; // 에디터까지 함께 정리한다
            openRow = null;
            if (window.history.state && window.history.state.feedbackId) {
                window.history.back();
            } else if (location.pathname !== detailBase) {
                window.history.replaceState(null, '', detailBase);
            }
        }

        /** 팝업 안의 저장/완료 버튼 처리. 성공하면 목록 행의 상태 태그도 함께 맞춘다. */
        function submitReply(form, action) {
            var id = form.getAttribute('data-id');
            var content = form.querySelector('[data-reply-content]').value;
            var buttons = form.querySelectorAll('button[type=submit]');
            buttons.forEach(function (b) { b.disabled = true; });

            G.postJson(detailBase + '/' + id + '/' + (action === 'complete' ? 'complete' : 'reply'), { content: content })
                .then(function (res) {
                    G.toast('저장되었습니다.');
                    if (openRow) {
                        var badge = openRow.querySelector('.gj-tags .gj-done, .gj-tags .gj-recv');
                        if (badge) {
                            badge.outerHTML = statusTag(res.status, res.processedAt === '' ? null : res.processedAt);
                        }
                    }
                    if (action === 'complete') {
                        // 완료 여부에 따라 팝업의 버튼 모양이 달라지므로 다시 받아 그린다.
                        openDetail(id, false, openRow);
                    } else {
                        buttons.forEach(function (b) { b.disabled = false; });
                    }
                })
                .catch(function (err) {
                    buttons.forEach(function (b) { b.disabled = false; });
                    G.toast(err.message, true);
                });
        }

        function bindPanel() {
            panel.querySelectorAll('[data-modal-close]').forEach(function (btn) {
                btn.addEventListener('click', closeModal);
            });
            var form = panel.querySelector('[data-reply-form]');
            if (!form) {
                return;
            }
            // quill-init 이 먼저 등록한 submit 리스너가 에디터 내용을 hidden 에 넣은 뒤 이 리스너가 실행된다.
            form.addEventListener('submit', function (e) {
                e.preventDefault();
                var action = e.submitter ? e.submitter.getAttribute('data-action') : 'save';
                submitReply(form, action);
            });
        }

        /** 상세 조각을 받아 팝업에 채운다. push=true 면 주소도 상세 주소로 바꾼다. */
        function openDetail(id, push, row) {
            openRow = row || openRow;
            fetch(detailBase + '/' + id + '/detail', { headers: { 'X-Requested-With': 'XMLHttpRequest' } })
                .then(function (res) {
                    if (!res.ok) {
                        throw new Error('의견을 불러오지 못했습니다.');
                    }
                    return res.text();
                })
                .then(function (html) {
                    panel.innerHTML = html;
                    modal.classList.remove('hidden');
                    if (window.QuillInit) {
                        window.QuillInit.init(panel);
                    }
                    bindPanel();
                    if (push) {
                        window.history.pushState({ feedbackId: id }, '', detailBase + '/' + id);
                    }
                })
                .catch(function (err) {
                    G.toast(err.message, true);
                });
        }

        // 보기 버튼으로 팝업을 연다(그리드가 다시 그려지므로 위임으로 건다).
        mount.addEventListener('click', function (e) {
            var btn = e.target.closest('[data-view]');
            if (!btn) {
                return;
            }
            openDetail(btn.getAttribute('data-view'), true, btn.closest('tr'));
        });

        modal.addEventListener('click', function (e) {
            if (e.target === modal) {
                closeModal();
            }
        });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape') {
                closeModal();
            }
        });
        window.addEventListener('popstate', function (e) {
            if (e.state && e.state.feedbackId) {
                openDetail(e.state.feedbackId, false, null);
            } else if (isOpen()) {
                modal.classList.add('hidden');
                panel.innerHTML = '';
                openRow = null;
            }
        });

        // 상세 주소로 직접 들어온 경우 해당 팝업을 열어둔다.
        var openId = mount.getAttribute('data-open-id');
        if (openId) {
            openDetail(openId, false, null);
        }
    });
})();

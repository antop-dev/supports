/**
 * 관리자 계정 관리 그리드.
 * 계정 추가/삭제만 가능하고 비밀번호는 이 화면에서 변경할 수 없다.
 */
(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        var mount = document.getElementById('grid');
        if (!mount || typeof gridjs === 'undefined') {
            return;
        }

        var G = window.AdminGrid;
        var dataUrl = mount.getAttribute('data-url'); // /ctx/admin/accounts/data
        var baseUrl = mount.getAttribute('data-base'); // /ctx/admin/accounts

        var state = { username: '' };

        function serverConfig() {
            return {
                url: dataUrl + G.queryString(state),
                then: function (data) {
                    return data.data.map(function (r) {
                        return [r.username, r.createdAt, r.id, r.self];
                    });
                },
                total: function (data) {
                    return data.total;
                }
            };
        }

        var columns = [
            {
                name: '아이디',
                formatter: function (cell, row) {
                    var me = row.cells[3].data
                        ? ' <span class="gj-badge gj-on">나</span>'
                        : '';
                    return gridjs.html('<span class="font-medium">' + G.escapeHtml(cell) + '</span>' + me);
                }
            },
            { name: '등록일시', width: '180px', formatter: function (cell) { return G.fmtDateTime(cell); } },
            {
                id: 'id',
                name: '관리',
                width: '120px',
                sort: false,
                formatter: function (cell, row) {
                    if (row.cells[3].data) {
                        // 로그인한 본인 계정은 삭제할 수 없다.
                        return gridjs.html('<button type="button" class="gj-icon-btn" disabled>' +
                            '<i class="fa-solid fa-trash"></i> 삭제</button>');
                    }
                    return gridjs.html(
                        '<button type="button" class="gj-icon-btn gj-del" data-id="' + G.escapeHtml(cell) + '"' +
                        ' data-name="' + G.escapeHtml(row.cells[0].data) + '">' +
                        '<i class="fa-solid fa-trash"></i> 삭제</button>'
                    );
                }
            },
            { id: 'self', name: 'self', hidden: true }
        ];

        var grid = new gridjs.Grid({
            columns: columns,
            server: serverConfig(),
            pagination: G.serverPagination(G.PAGE_SIZE),
            fixedHeader: true,
            language: window.gridjsLocaleKr
        }).render(mount);

        mount.addEventListener('click', function (e) {
            var btn = e.target.closest('.gj-del');
            if (!btn) {
                return;
            }
            var id = btn.getAttribute('data-id');
            var name = btn.getAttribute('data-name');
            if (!window.confirm('“' + name + '” 계정을 삭제하시겠습니까?')) {
                return;
            }
            btn.disabled = true;
            G.postJson(baseUrl + '/' + id + '/delete', {})
                .then(function () {
                    G.toast('삭제되었습니다.');
                    grid.forceRender();
                })
                .catch(function (err) {
                    btn.disabled = false;
                    G.toast(err.message, true);
                });
        });

        function applySearch() {
            state.username = document.getElementById('q-username').value.trim();
            grid.updateConfig({ server: serverConfig(), pagination: G.serverPagination(G.PAGE_SIZE) }).forceRender();
        }

        document.getElementById('searchForm').addEventListener('submit', function (e) {
            e.preventDefault();
            applySearch();
        });
        document.getElementById('resetBtn').addEventListener('click', function () {
            setTimeout(applySearch, 0);
        });

        // ---------- 등록 팝업 ----------

        var modal = document.getElementById('accountModal');
        var errorEl = document.getElementById('a-error');

        function openModal() {
            document.getElementById('a-username').value = '';
            document.getElementById('a-password').value = '';
            errorEl.classList.add('hidden');
            modal.classList.remove('hidden');
            document.getElementById('a-username').focus();
        }

        function closeModal() {
            modal.classList.add('hidden');
        }

        document.getElementById('addBtn').addEventListener('click', openModal);
        modal.querySelectorAll('[data-modal-close]').forEach(function (btn) {
            btn.addEventListener('click', closeModal);
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

        document.getElementById('a-save').addEventListener('click', function () {
            var btn = this;
            btn.disabled = true;
            errorEl.classList.add('hidden');
            G.postJson(baseUrl, {
                username: document.getElementById('a-username').value,
                password: document.getElementById('a-password').value
            }).then(function () {
                btn.disabled = false;
                closeModal();
                G.toast('등록되었습니다.');
                grid.forceRender();
            }).catch(function (err) {
                btn.disabled = false;
                errorEl.textContent = err.message;
                errorEl.classList.remove('hidden');
            });
        });
    });
})();

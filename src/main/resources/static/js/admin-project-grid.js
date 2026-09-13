/**
 * 관리자 프로젝트 관리 그리드.
 * - 셀을 클릭하면 그 자리에서 수정하고, 값이 바뀌면 즉시 저장한다.
 * - 상단 "+" 버튼으로 등록 팝업을 연다.
 */
(function () {
    'use strict';

    document.addEventListener('DOMContentLoaded', function () {
        var mount = document.getElementById('grid');
        if (!mount || typeof gridjs === 'undefined') {
            return;
        }

        var G = window.AdminGrid;
        var dataUrl = mount.getAttribute('data-url'); // /ctx/admin/projects/data
        var baseUrl = mount.getAttribute('data-base'); // /ctx/admin/projects
        var formUrl = mount.getAttribute('data-form'); // /ctx/feedbacks/new

        /** 해당 프로젝트가 미리 선택된 접수 폼 주소. 관리자가 복사해 각 서비스에 걸어둔다. */
        function shareLink(code) {
            return window.location.origin + formUrl + '?project=' + encodeURIComponent(code);
        }

        var state = { name: '', enabled: '' };

        // ---------- 셀 렌더링 ----------

        function enabledBadge(value) {
            return value === 'true' || value === true
                ? '<span class="gj-badge gj-on"><i class="fa-solid fa-circle-check"></i> 활성</span>'
                : '<span class="gj-badge gj-off"><i class="fa-regular fa-circle"></i> 비활성</span>';
        }

        function display(field, value) {
            if (field === 'enabled') {
                return enabledBadge(value);
            }
            if (field === 'url') {
                if (!value) {
                    return '<span class="gj-muted">-</span>';
                }
                var href = G.escapeHtml(value);
                return '<a class="gj-link" href="' + href + '" target="_blank" rel="noopener noreferrer">' + href + '</a>';
            }
            return G.escapeHtml(value);
        }

        /** 코드 셀: 값은 그 자리에서 수정하고, 옆 버튼으로 접수 폼 링크를 복사한다. */
        function codeCell(cell, row) {
            var id = row.cells[ID_INDEX].data;
            var value = String(cell);
            return gridjs.html(
                '<span class="gj-code-cell">' +
                '<span class="gj-edit" data-id="' + G.escapeHtml(id) + '" data-field="code"' +
                ' data-value="' + G.escapeHtml(value) + '">' + G.escapeHtml(value) + '</span>' +
                '<button type="button" class="gj-icon-btn gj-copy" title="접수 폼 링크 복사"' +
                ' data-code="' + G.escapeHtml(value) + '"><i class="fa-solid fa-link"></i></button>' +
                '</span>'
            );
        }

        function copyShareLink(btn) {
            var link = shareLink(btn.getAttribute('data-code'));
            function done() {
                G.toast('접수 폼 링크를 복사했습니다.');
            }
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(link).then(done, done);
                return;
            }
            var ta = document.createElement('textarea');
            ta.value = link;
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

        /** 수정 가능한 셀은 id/필드/현재값을 들고 있는 span 으로 그린다. */
        function editableCell(field) {
            return function (cell, row) {
                var id = row.cells[ID_INDEX].data;
                var value = String(cell);
                return gridjs.html(
                    '<span class="gj-edit" data-id="' + G.escapeHtml(id) + '"' +
                    ' data-field="' + field + '"' +
                    ' data-value="' + G.escapeHtml(value) + '">' + display(field, value) + '</span>'
                );
            };
        }

        function renderValue(span, field, value) {
            span.innerHTML = display(field, value);
            if (field === 'code') {
                // 코드를 고쳤으면 옆 복사 버튼이 들고 있는 값도 같이 바꾼다.
                var copyBtn = span.parentNode && span.parentNode.querySelector('.gj-copy');
                if (copyBtn) {
                    copyBtn.setAttribute('data-code', value);
                }
            }
        }

        // ---------- 셀 단위 수정 ----------

        function startEdit(span) {
            if (span.querySelector('.gj-edit-input')) {
                return;
            }
            var field = span.getAttribute('data-field');
            var id = span.getAttribute('data-id');
            var value = span.getAttribute('data-value');
            var input;

            if (field === 'enabled') {
                input = document.createElement('select');
                input.innerHTML = '<option value="true">활성</option><option value="false">비활성</option>';
                input.value = value;
            } else {
                input = document.createElement('input');
                input.type = field === 'sortOrder' ? 'number' : 'text';
                input.value = value;
                if (field === 'name') {
                    input.maxLength = 100;
                } else if (field === 'code') {
                    input.maxLength = 50;
                }
            }
            input.className = 'gj-edit-input';
            span.innerHTML = '';
            span.appendChild(input);
            input.focus();
            if (input.select) {
                input.select();
            }

            var finished = false;

            function finish(save) {
                if (finished) {
                    return;
                }
                finished = true;
                var newValue = String(input.value);
                if (!save || newValue === value) {
                    renderValue(span, field, value);
                    return;
                }
                span.classList.add('gj-saving');
                G.postJson(baseUrl + '/' + id + '/cell', { field: field, value: newValue })
                    .then(function (res) {
                        span.classList.remove('gj-saving');
                        var saved = String(res.value);
                        span.setAttribute('data-value', saved);
                        renderValue(span, field, saved);
                        G.toast('저장되었습니다.');
                        if (field === 'sortOrder') {
                            // 정렬 순서가 바뀌면 목록 순서도 다시 맞춘다.
                            grid.forceRender();
                        }
                    })
                    .catch(function (err) {
                        span.classList.remove('gj-saving');
                        renderValue(span, field, value);
                        G.toast(err.message, true);
                    });
            }

            input.addEventListener('blur', function () { finish(true); });
            input.addEventListener('keydown', function (e) {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    finish(true);
                } else if (e.key === 'Escape') {
                    e.preventDefault();
                    finish(false);
                }
            });
            if (field === 'enabled') {
                input.addEventListener('change', function () { finish(true); });
            }
        }

        function removeProject(btn) {
            var id = btn.getAttribute('data-id');
            var name = btn.getAttribute('data-name');
            if (!window.confirm('“' + name + '” 프로젝트를 삭제하시겠습니까?')) {
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
        }

        mount.addEventListener('click', function (e) {
            var del = e.target.closest('.gj-del');
            if (del) {
                removeProject(del);
                return;
            }
            var copyBtn = e.target.closest('.gj-copy');
            if (copyBtn) {
                copyShareLink(copyBtn);
                return;
            }
            if (e.target.closest('.gj-link')) {
                // URL 링크는 새 탭으로 열게 두고, 편집 모드로 들어가지 않는다.
                return;
            }
            var span = e.target.closest('.gj-edit');
            if (span) {
                startEdit(span);
            }
        });

        // ---------- 그리드 ----------

        var ID_INDEX = 6;

        function serverConfig() {
            return {
                url: dataUrl + G.queryString(state),
                then: function (data) {
                    return data.data.map(function (r) {
                        return [r.name, r.code, r.url, r.sortOrder, String(r.enabled), r.createdAt, r.id];
                    });
                },
                total: function (data) {
                    return data.total;
                }
            };
        }

        var columns = [
            { name: '프로젝트명', formatter: editableCell('name') },
            { name: '코드', width: '190px', formatter: codeCell },
            { name: 'URL', formatter: editableCell('url') },
            { name: '정렬 순서', width: '110px', formatter: editableCell('sortOrder') },
            { name: '활성화', width: '120px', formatter: editableCell('enabled') },
            { name: '등록일시', width: '150px', formatter: function (cell) { return G.fmtDateTime(cell); } },
            {
                id: 'id',
                name: '',
                width: '100px',
                sort: false,
                formatter: function (cell, row) {
                    return gridjs.html(
                        '<button type="button" class="gj-icon-btn gj-del" data-id="' + G.escapeHtml(cell) + '"' +
                        ' data-name="' + G.escapeHtml(row.cells[0].data) + '">' +
                        '<i class="fa-solid fa-trash"></i> 삭제</button>'
                    );
                }
            }
        ];

        var grid = new gridjs.Grid({
            columns: columns,
            server: serverConfig(),
            pagination: G.serverPagination(G.PAGE_SIZE),
            fixedHeader: true,
            language: window.gridjsLocaleKr
        }).render(mount);

        function applySearch() {
            state.name = document.getElementById('q-name').value.trim();
            state.enabled = document.getElementById('q-enabled').value;
            grid.updateConfig({ server: serverConfig(), pagination: G.serverPagination(G.PAGE_SIZE) }).forceRender();
        }

        document.getElementById('searchForm').addEventListener('submit', function (e) {
            e.preventDefault();
            applySearch();
        });
        document.getElementById('q-enabled').addEventListener('change', applySearch);
        document.getElementById('resetBtn').addEventListener('click', function () {
            setTimeout(applySearch, 0);
        });

        // ---------- 등록 팝업 ----------

        var modal = document.getElementById('projectModal');
        var errorEl = document.getElementById('p-error');

        function openModal() {
            document.getElementById('p-name').value = '';
            document.getElementById('p-code').value = '';
            document.getElementById('p-sortOrder').value = '0';
            document.getElementById('p-url').value = '';
            document.getElementById('p-enabled').checked = true;
            errorEl.classList.add('hidden');
            modal.classList.remove('hidden');
            document.getElementById('p-name').focus();
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

        document.getElementById('p-save').addEventListener('click', function () {
            var btn = this;
            btn.disabled = true;
            errorEl.classList.add('hidden');
            G.postJson(baseUrl, {
                name: document.getElementById('p-name').value,
                code: document.getElementById('p-code').value,
                sortOrder: parseInt(document.getElementById('p-sortOrder').value, 10) || 0,
                enabled: document.getElementById('p-enabled').checked,
                url: document.getElementById('p-url').value
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

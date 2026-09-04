(function () {
    'use strict';

    // 프로젝트 선택: readonly 트리거 입력을 클릭하면 팝업이 뜨고, 팝업에서 프로젝트(URL 링크 포함)를 골라 선택한다.
    document.addEventListener('DOMContentLoaded', function () {
        var hiddenInput = document.getElementById('projectId');
        var trigger = document.getElementById('projectTrigger');
        var source = document.getElementById('projectSource');
        if (!hiddenInput || !trigger || !source) {
            return;
        }

        var projects = Array.prototype.map.call(source.content.querySelectorAll('span'), function (el) {
            return {
                id: el.getAttribute('data-id') || '',
                name: el.getAttribute('data-name') || '',
                url: el.getAttribute('data-url') || ''
            };
        });

        function escapeHtml(s) {
            return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) {
                return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
            });
        }

        function selectProject(project) {
            hiddenInput.value = project.id;
            trigger.value = project.name;
        }

        // ---------- 프로젝트 선택 팝업 ----------

        var modal = document.getElementById('projectModal');
        var modalSearch = document.getElementById('projectModalSearch');
        var modalList = document.getElementById('projectModalList');
        var closeBtn = document.getElementById('projectModalClose');

        function renderModalList(query) {
            var q = (query || '').trim().toLowerCase();
            var matches = q
                ? projects.filter(function (p) { return p.name.toLowerCase().indexOf(q) !== -1; })
                : projects;

            if (matches.length === 0) {
                modalList.innerHTML = '<li class="is-empty">' + escapeHtml(modalList.getAttribute('data-empty')) + '</li>';
                return;
            }
            modalList.innerHTML = matches.map(function (p) {
                var urlHtml = p.url
                    ? '<a class="project-list-url" href="' + escapeHtml(p.url) + '" target="_blank" rel="noopener noreferrer">' + escapeHtml(p.url) + '</a>'
                    : '';
                return '<li data-id="' + escapeHtml(p.id) + '">' +
                    '<span class="project-list-name">' + escapeHtml(p.name) + '</span>' + urlHtml +
                    '</li>';
            }).join('');
        }

        function openModal() {
            modalSearch.value = '';
            renderModalList('');
            modal.hidden = false;
            modalSearch.focus();
        }

        function closeModal() {
            modal.hidden = true;
            trigger.focus();
        }

        var triggerWrap = document.getElementById('projectTriggerWrap');
        triggerWrap.addEventListener('click', openModal);
        trigger.addEventListener('keydown', function (e) {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                openModal();
            }
        });

        if (closeBtn) {
            closeBtn.addEventListener('click', closeModal);
        }
        modal.addEventListener('click', function (e) {
            if (e.target === modal) {
                closeModal();
            }
        });
        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && !modal.hidden) {
                closeModal();
            }
        });
        modalSearch.addEventListener('input', function () {
            renderModalList(modalSearch.value);
        });
        modalList.addEventListener('click', function (e) {
            if (e.target.closest('.project-list-url')) {
                // URL 링크는 새 탭으로 열게 두고, 선택 처리는 하지 않는다.
                return;
            }
            var li = e.target.closest('li[data-id]');
            if (!li) {
                return;
            }
            var project = projects.filter(function (p) { return p.id === li.getAttribute('data-id'); })[0];
            if (project) {
                selectProject(project);
                closeModal();
            }
        });
    });
})();

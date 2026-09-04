(function () {
    'use strict';

    const STORAGE_KEY = 'app-theme'; // 'light' | 'dark' | null(시스템)

    function stored() {
        try {
            return localStorage.getItem(STORAGE_KEY);
        } catch (e) {
            return null;
        }
    }

    function effectiveTheme() {
        const s = stored();
        if (s === 'light' || s === 'dark') {
            return s;
        }
        return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }

    // 저장된 값이 있으면 data-theme 적용(시스템이면 속성 제거하여 매체쿼리에 위임).
    // Tailwind(class 기반, 관리자 페이지)와 아이콘을 위해 dark 클래스는 실제 적용 테마 기준으로 토글.
    function applyStored() {
        const root = document.documentElement;
        const s = stored();
        if (s === 'light' || s === 'dark') {
            root.setAttribute('data-theme', s);
        } else {
            root.removeAttribute('data-theme');
        }
        root.classList.toggle('dark', effectiveTheme() === 'dark');
    }

    function toggle() {
        const next = effectiveTheme() === 'dark' ? 'light' : 'dark';
        try {
            localStorage.setItem(STORAGE_KEY, next);
        } catch (e) { /* ignore */ }
        applyStored();
        updateIcons();
    }

    function updateIcons() {
        const isDark = effectiveTheme() === 'dark';
        document.querySelectorAll('[data-theme-icon]').forEach(function (el) {
            el.textContent = isDark ? 'light_mode' : 'dark_mode';
        });
        document.querySelectorAll('[data-theme-icon-fa]').forEach(function (el) {
            el.className = isDark ? 'fa-solid fa-sun' : 'fa-solid fa-moon';
        });
    }

    applyStored();

    // 시스템 모드일 때 OS 테마 변경에 반응
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function () {
        if (!stored()) {
            applyStored();
            updateIcons();
        }
    });

    document.addEventListener('DOMContentLoaded', function () {
        updateIcons();
        document.querySelectorAll('[data-theme-toggle]').forEach(function (btn) {
            btn.addEventListener('click', toggle);
        });
    });
})();

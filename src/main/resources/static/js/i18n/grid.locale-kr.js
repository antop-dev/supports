/**
 * Grid.js 한국어 언어 팩.
 * window.gridjsLocaleKr 로 노출하며, Grid 생성 시 language 옵션에 그대로 넘겨 쓴다.
 */
(function (global) {
    'use strict';

    global.gridjsLocaleKr = {
        search: {
            placeholder: '검색어를 입력하세요'
        },
        sort: {
            sortAsc: '오름차순 정렬',
            sortDesc: '내림차순 정렬'
        },
        pagination: {
            previous: '이전',
            next: '다음',
            firstPage: '처음',
            lastPage: '마지막',
            navigate: function (page, pages) {
                return pages + '페이지 중 ' + page + '페이지';
            },
            page: function (page) {
                return page + '페이지';
            },
            // "1 ~ 20 / 4751 건" 형태로 표시된다.
            // Grid.js 는 빈 문자열을 "번역 없음"으로 보고 영어 원문(Showing)을 쓰므로 폭 없는 공백을 넣는다.
            showing: '​',
            to: '~',
            of: '/',
            results: '건'
        },
        loading: '불러오는 중…',
        noRecordsFound: '데이터가 없습니다',
        error: '데이터를 불러오지 못했습니다'
    };
})(window);

package ai.antop.supports.web

/**
 * 검색 엔진에 색인을 허용하는 화면. 여기 없는 주소는 전부 비색인이다.
 *
 * robots.txt / sitemap.xml 과 페이지의 `<meta name="robots">` 가 모두 이 목록 하나를 본다.
 * 두 군데에 따로 적어두면 한쪽만 고쳐져 사용자 제출 내용이 색인되는 사고가 난다.
 */
object SeoPaths {
    /** 컨텍스트 경로를 뺀 경로. 진입 화면만 허용하고 개별 의견·완료·관리 화면은 제외한다. */
    val INDEXABLE = listOf("/", "/feedbacks/new", "/feedbacks/lookup")

    /** 크롤러가 화면을 제대로 렌더링하려면 정적 자원은 열어줘야 한다. */
    val CRAWLABLE_ASSETS = listOf("/css/", "/js/", "/vendor/")

    fun isIndexable(path: String): Boolean = path in INDEXABLE
}

package ai.antop.supports.common

import org.jsoup.Jsoup
import org.jsoup.safety.Safelist

/** WYSIWYG 에디터 HTML 을 안전하게 정제한다(XSS 방지). */
object HtmlSanitizer {
    private val safelist: Safelist =
        Safelist
            .relaxed()
            // 정렬 등 에디터 서식 클래스 유지(class 는 스크립트 실행 불가)
            .addAttributes(":all", "class")

    fun sanitize(html: String): String = Jsoup.clean(html, safelist)

    /** 태그를 제거한 순수 텍스트. 내용 필수 검증에 사용. */
    fun plainText(html: String): String = Jsoup.parse(html).text().trim()

    fun isBlank(html: String): Boolean = plainText(html).isBlank()
}

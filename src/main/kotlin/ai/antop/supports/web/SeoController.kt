package ai.antop.supports.web

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody

/**
 * 크롤러용 파일을 만들어 내려준다. 도메인이 배포마다 달라지므로 정적 파일로 두지 않고
 * 요청 기준(또는 `app.base-url`)으로 절대 주소를 채운다.
 *
 * 이 앱은 컨텍스트 경로(`/supports`) 아래에서 돌기 때문에 여기서 내려주는 주소도
 * `/supports/robots.txt` 다. 크롤러는 도메인 루트만 보므로 앞단(nginx 등)에서
 * `/robots.txt` 를 이 주소로 연결해야 한다(README 참고).
 */
@Controller
class SeoController(
    private val urlBuilder: UrlBuilder,
) {
    @GetMapping("/robots.txt", produces = [MediaType.TEXT_PLAIN_VALUE])
    @ResponseBody
    fun robots(request: HttpServletRequest): String {
        val ctx = request.contextPath
        val rules =
            buildString {
                // 진입 화면만 열고, 그 아래는 전부 막는다. 더 긴 경로가 우선하므로 Allow 가 이긴다.
                SeoPaths.INDEXABLE.forEach {
                    appendLine("Allow: $ctx${if (it == "/") "/$" else it}")
                }
                // 화면을 제대로 렌더링하려면 정적 자원은 읽을 수 있어야 한다.
                SeoPaths.CRAWLABLE_ASSETS.forEach { appendLine("Allow: $ctx$it") }
                // 관리 화면·개별 의견·첨부 파일을 한 줄로 덮는다.
                // 경로를 하나씩 적으면 오히려 관리자 주소를 광고하는 셈이 된다.
                appendLine("Disallow: $ctx/")
            }
        return buildString {
            appendLine("# 의견 접수(supports)")
            appendLine("# 색인 허용: 의견 등록 / 접수 확인 진입 화면")
            appendLine("# 차단: 개별 의견·완료 화면·첨부 파일·관리 화면(사용자 제출 내용 보호)")
            appendLine()
            appendLine("User-agent: *")
            append(rules)
            appendLine()
            appendLine("# 생성형 AI 검색. Google-Extended 는 자기 그룹이 없으면 규칙을 적용받지 않으므로")
            appendLine("# 같은 범위를 다시 적어준다. 나머지 AI 크롤러는 위 User-agent: * 를 따른다.")
            appendLine("User-agent: Google-Extended")
            append(rules)
            appendLine()
            appendLine("Sitemap: ${urlBuilder.absolute(request, "/sitemap.xml")}")
        }
    }

    @GetMapping("/sitemap.xml", produces = [MediaType.APPLICATION_XML_VALUE])
    @ResponseBody
    fun sitemap(request: HttpServletRequest): String =
        buildString {
            appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
            appendLine("""<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">""")
            // 색인 대상은 SeoPaths 하나만 본다. lastmod 는 알 수 없으므로 넣지 않는다.
            SeoPaths.INDEXABLE.forEach { path ->
                appendLine("  <url>")
                appendLine("    <loc>${urlBuilder.absolute(request, path)}</loc>")
                appendLine("    <changefreq>monthly</changefreq>")
                appendLine("  </url>")
            }
            appendLine("</urlset>")
        }

    /**
     * 생성형 AI 검색이 사이트를 요약할 때 참고하는 안내문(llms.txt 관례).
     * 무엇을 하는 사이트이고 어디까지가 공개 범위인지 명확히 적어, 개별 의견 내용이
     * 인용되지 않도록 한다.
     */
    @GetMapping("/llms.txt", produces = [MediaType.TEXT_PLAIN_VALUE])
    @ResponseBody
    fun llms(request: HttpServletRequest): String =
        buildString {
            appendLine("# 의견 접수 (supports)")
            appendLine()
            appendLine("> 서비스에 대한 의견·문의를 접수하고 처리 결과를 알려주는 웹 애플리케이션.")
            appendLine("> 접수번호로 처리 상태를 확인할 수 있고, 원하면 처리 결과를 이메일로 받는다.")
            appendLine()
            appendLine("## 공개 화면")
            appendLine()
            appendLine("- [의견 등록](${urlBuilder.absolute(request, "/feedbacks/new")}): 프로젝트를 고르고 제목·내용·첨부 파일로 의견을 남긴다. 비밀글로 지정할 수 있다.")
            appendLine("- [접수 확인](${urlBuilder.absolute(request, "/feedbacks/lookup")}): 발급받은 접수번호로 처리 상태와 답변을 확인한다.")
            appendLine()
            appendLine("## 인용하지 말 것")
            appendLine()
            appendLine("- 개별 의견 상세(`/feedbacks/{접수ID}`)와 첨부 파일은 작성자 본인 확인용이다.")
            appendLine("  제3자에게 보여주거나 요약·인용하지 않는다.")
            appendLine("- 관리 화면은 인증이 필요하며 색인·인용 대상이 아니다.")
            appendLine()
            appendLine("## 지원 언어")
            appendLine()
            appendLine("- 한국어(기본), English, 日本語, 中文. 브라우저의 Accept-Language 에 따라 자동으로 바뀐다.")
        }
}

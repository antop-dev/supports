package ai.antop.supports.web

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

/**
 * CSRF 토큰이 없거나 세션이 만료돼 검증에 실패했을 때의 응답.
 *
 * 화면 대부분이 AJAX 라서 403 HTML 을 돌려주면 클라이언트가 응답을 읽지 못한다.
 * 그래서 AJAX/JSON 요청에는 각 화면이 이미 쓰고 있는 오류 형식(JSON)으로 알려주고,
 * 일반 폼 전송(주소창 이동)일 때만 403 화면으로 보낸다.
 */
@Component
class CsrfAccessDeniedHandler : AccessDeniedHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) {
        log.warn("Access denied (CSRF 추정): {} {}", request.method, request.requestURI)
        response.status = HttpStatus.FORBIDDEN.value()
        if (!expectsHtml(request)) {
            response.characterEncoding = StandardCharsets.UTF_8.name()
            response.contentType = MediaType.APPLICATION_JSON_VALUE
            // globalErrors/fieldErrors 는 사용자 화면이, message 는 관리자 화면이 읽는 자리다.
            response.writer.write(
                """{"ok":false,"message":"$MESSAGE","globalErrors":["$MESSAGE"],"fieldErrors":{}}""",
            )
            return
        }
        request.getRequestDispatcher("/error/403").forward(request, response)
    }

    /** 브라우저 주소창에서 온 화면 요청인지. AJAX 는 JSON 을 기대한다. */
    private fun expectsHtml(request: HttpServletRequest): Boolean {
        if (request.getHeader("X-Requested-With") == "XMLHttpRequest") {
            return false
        }
        val contentType = request.contentType
        if (contentType != null && contentType.startsWith(MediaType.APPLICATION_JSON_VALUE)) {
            return false
        }
        return request.getHeader(HttpHeaders.ACCEPT)?.contains(MediaType.TEXT_HTML_VALUE) == true
    }

    companion object {
        private const val MESSAGE = "요청이 만료되었습니다. 페이지를 새로고침한 뒤 다시 시도해 주세요."
    }
}

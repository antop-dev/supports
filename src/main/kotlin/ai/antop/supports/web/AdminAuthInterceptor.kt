package ai.antop.supports.web

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AdminAuthInterceptor : HandlerInterceptor {
    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val loggedIn = request.getSession(false)?.getAttribute(SessionKeys.ADMIN) != null
        if (loggedIn) {
            return true
        }
        rememberTarget(request)
        response.sendRedirect("${request.contextPath}/admin/login")
        return false
    }

    /**
     * 로그인 후 원래 보려던 화면으로 돌려보내기 위해 주소를 세션에 담아둔다.
     * 화면 이동이 아닌 요청(AJAX 데이터 조회 등)은 담지 않는다.
     */
    private fun rememberTarget(request: HttpServletRequest) {
        if (!isPageRequest(request)) {
            return
        }
        val target =
            buildString {
                append(request.requestURI)
                request.queryString?.let { append('?').append(it) }
            }
        request.getSession(true).setAttribute(SessionKeys.ADMIN_REDIRECT, target)
    }

    /** 브라우저 주소창으로 들어온 화면 요청인지. */
    private fun isPageRequest(request: HttpServletRequest): Boolean {
        if (!HttpMethod.GET.matches(request.method)) {
            return false
        }
        return request.getHeader("Accept")?.contains("text/html") == true
    }
}

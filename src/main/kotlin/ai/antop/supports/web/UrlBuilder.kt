package ai.antop.supports.web

import ai.antop.supports.config.AppProperties
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

/**
 * 메일·슬랙처럼 화면 밖으로 나가는 곳에 넣을 절대 주소를 만든다.
 * `app.base-url` 이 있으면 그 값을 쓰고, 없으면 현재 요청을 기준으로 만든다.
 */
@Component
class UrlBuilder(
    private val appProperties: AppProperties,
) {
    /** 접수자가 자기 의견을 확인하는 화면 주소. */
    fun feedbackView(
        request: HttpServletRequest,
        id: String,
    ): String = "${base(request)}/feedbacks/$id"

    /** 관리자 상세 화면 주소. */
    fun adminFeedback(
        request: HttpServletRequest,
        id: String,
    ): String = "${base(request)}/admin/feedbacks/$id"

    /** 임의 경로(컨텍스트 경로 제외)의 절대 주소. canonical·sitemap 처럼 화면 밖으로 나가는 주소에 쓴다. */
    fun absolute(
        request: HttpServletRequest,
        path: String,
    ): String = base(request) + path.removeSuffix("/").ifEmpty { "/" }

    private fun base(request: HttpServletRequest): String =
        if (appProperties.baseUrl.isNotBlank()) {
            appProperties.baseUrl.trimEnd('/') + request.contextPath
        } else {
            ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()
        }
}

package ai.antop.supports.web

import ai.antop.supports.config.AppProperties
import jakarta.servlet.http.HttpServletRequest
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ModelAttribute

/** 모든 화면에 공통으로 필요한 모델을 주입한다. */
@ControllerAdvice
class GlobalModelAdvice(
    private val appProperties: AppProperties,
    private val urlBuilder: UrlBuilder,
) {
    @ModelAttribute
    fun addCommonAttributes(
        model: Model,
        request: HttpServletRequest,
    ) {
        model.addAttribute("maxFiles", appProperties.upload.maxFiles)
        model.addAttribute("maxFileSizeBytes", appProperties.upload.maxFileSizeBytes)

        // 색인 여부를 화면마다 정하지 않고 여기서 한 번에 정한다([SeoPaths]).
        // 개별 의견·완료·비밀글 확인·오류 화면은 자동으로 noindex 가 된다.
        val path = request.requestURI.removePrefix(request.contextPath).ifEmpty { "/" }
        model.addAttribute("noIndex", !SeoPaths.isIndexable(path))
        model.addAttribute("canonicalUrl", urlBuilder.absolute(request, path))
    }
}

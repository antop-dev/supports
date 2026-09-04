package ai.antop.supports.web

import ai.antop.supports.config.AppProperties
import jakarta.servlet.http.HttpSession
import org.springframework.stereotype.Component

/** 세션에 저장된 캡차 코드를 검증한다. 검증 후에는 재사용을 막기 위해 코드를 제거한다. */
@Component
class CaptchaValidator(
    private val appProperties: AppProperties,
) {
    fun validate(
        session: HttpSession,
        input: String?,
    ): Boolean {
        val expected = session.getAttribute(SessionKeys.CAPTCHA) as? String
        session.removeAttribute(SessionKeys.CAPTCHA)
        if (!appProperties.captcha.enabled) {
            return true
        }
        return !expected.isNullOrBlank() && expected == input?.trim()
    }
}

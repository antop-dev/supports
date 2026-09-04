package ai.antop.supports.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

/**
 * CSRF 검증 실패([CsrfAccessDeniedHandler])가 화면 요청일 때 포워드되는 자리.
 * 포워드는 원래 요청의 메서드(POST 등)를 그대로 유지하므로 메서드를 제한하지 않는다.
 */
@Controller
class ErrorPageController {
    @RequestMapping("/error/403")
    fun forbidden(): String = "error/403"
}

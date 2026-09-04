package ai.antop.supports.web

import ai.antop.supports.service.CaptchaService
import jakarta.servlet.http.HttpSession
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CaptchaController(
    private val captchaService: CaptchaService,
) {
    @GetMapping("/captcha")
    fun captcha(session: HttpSession): ResponseEntity<ByteArray> {
        val result = captchaService.generate()
        session.setAttribute(SessionKeys.CAPTCHA, result.code)
        return ResponseEntity
            .ok()
            .contentType(MediaType.IMAGE_PNG)
            .cacheControl(CacheControl.noStore())
            .body(result.png)
    }
}

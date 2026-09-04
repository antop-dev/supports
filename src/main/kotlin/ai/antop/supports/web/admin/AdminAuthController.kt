package ai.antop.supports.web.admin

import ai.antop.supports.dto.LoginForm
import ai.antop.supports.dto.LoginResponse
import ai.antop.supports.dto.PasswordChangeForm
import ai.antop.supports.service.AdminUserService
import ai.antop.supports.web.CaptchaValidator
import ai.antop.supports.web.SessionKeys
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import org.springframework.context.MessageSourceResolvable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@RequestMapping("/admin")
class AdminAuthController(
    private val adminUserService: AdminUserService,
    private val captchaValidator: CaptchaValidator,
) {
    /** 관리자 첫 화면. 주소창에 /admin 만 친 경우 의견 목록으로 보낸다. */
    @GetMapping("", "/")
    fun index(): String = "redirect:/admin/feedbacks"

    @GetMapping("/login")
    fun loginForm(session: HttpSession): String {
        if (session.getAttribute(SessionKeys.ADMIN) != null) {
            return "redirect:/admin/feedbacks"
        }
        return "admin/login"
    }

    /**
     * 로그인은 화면 이동 없이 AJAX 로 처리한다. 실패해도 페이지를 다시 그리지 않으므로
     * 오류를 항목별 메시지로 내려주고, 화면이 그 자리에 표시한다.
     */
    @PostMapping("/login")
    @ResponseBody
    fun login(
        @Valid @RequestBody form: LoginForm,
        bindingResult: BindingResult,
        session: HttpSession,
        request: HttpServletRequest,
    ): ResponseEntity<LoginResponse> {
        if (!captchaValidator.validate(session, form.captcha)) {
            bindingResult.rejectValue("captcha", "captcha.invalid", "자동입력 방지 문자가 일치하지 않습니다.")
        }
        if (!bindingResult.hasErrors() && !adminUserService.authenticate(form.username, form.password)) {
            bindingResult.reject("login.failed", "아이디 또는 비밀번호가 올바르지 않습니다.")
        }
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(
                LoginResponse(
                    ok = false,
                    message = bindingResult.globalErrors.firstOrNull()?.let(::message),
                    // 한 항목에 오류가 겹치면(예: 미입력 + 캡차 불일치) 먼저 걸린 것만 보여준다.
                    errors =
                        bindingResult.fieldErrors
                            .groupBy(FieldError::getField)
                            .mapValues { (_, errors) -> message(errors.first()) },
                ),
            )
        }
        // 세션 고정 방어. 로그인 전에 발급된 세션 ID 를 그대로 두면, 그 ID 를 미리 심어둔
        // 공격자가 로그인 이후의 세션을 그대로 쓸 수 있다.
        // invalidate() 대신 changeSessionId() 를 쓰는 이유는 아래 popRedirect() 가 읽는
        // 세션 속성(로그인 전에 가려던 주소)을 유지해야 하기 때문이다.
        request.changeSessionId()
        session.setAttribute(SessionKeys.ADMIN, form.username.trim())
        return ResponseEntity.ok(LoginResponse(ok = true, redirect = popRedirect(session)))
    }

    /** 로그인 전에 들어오려던 관리자 화면 주소를 한 번만 꺼내 쓴다. */
    private fun popRedirect(session: HttpSession): String? {
        val target = session.getAttribute(SessionKeys.ADMIN_REDIRECT) as? String
        session.removeAttribute(SessionKeys.ADMIN_REDIRECT)
        return target
    }

    @PostMapping("/logout")
    fun logout(session: HttpSession): String {
        session.invalidate()
        return "redirect:/admin/login"
    }

    /** 비밀번호 변경은 상단 메뉴의 레이어 팝업으로 처리한다. 예전 주소로 들어오면 목록으로 보낸다. */
    @GetMapping("/password")
    fun passwordForm(): String = "redirect:/admin/feedbacks"

    /** 팝업에서 로그인한 본인의 비밀번호를 변경한다. 성공하면 세션을 끊어 다시 로그인하게 한다. */
    @PostMapping("/password")
    @ResponseBody
    fun changePassword(
        @RequestBody form: PasswordChangeForm,
        session: HttpSession,
    ): ResponseEntity<Map<String, Any>> {
        val username =
            session.getAttribute(SessionKeys.ADMIN) as? String
                ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorBody("다시 로그인해 주세요."))
        return runCatching {
            require(form.currentPassword.isNotBlank()) { "현재 비밀번호를 입력하세요." }
            require(form.newPassword == form.confirmPassword) { "새 비밀번호가 서로 일치하지 않습니다." }
            adminUserService.changePassword(username, form.currentPassword, form.newPassword)
        }.fold(
            onSuccess = {
                // 비밀번호가 바뀌었으므로 현재 세션을 끊는다(= 로그아웃).
                session.invalidate()
                ResponseEntity.ok(mapOf("ok" to true))
            },
            onFailure = { ResponseEntity.badRequest().body(errorBody(it.message ?: "비밀번호를 변경하지 못했습니다.")) },
        )
    }

    private fun errorBody(message: String): Map<String, Any> = mapOf("ok" to false, "message" to message)

    /** 관리자 화면은 다국어를 쓰지 않으므로 검증에 붙여 둔 문구를 그대로 쓴다. */
    private fun message(resolvable: MessageSourceResolvable): String = resolvable.defaultMessage ?: "입력값을 확인해 주세요."
}

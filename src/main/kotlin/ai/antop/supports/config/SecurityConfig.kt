package ai.antop.supports.config

import ai.antop.supports.web.CsrfAccessDeniedHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
import org.springframework.security.web.header.writers.StaticHeadersWriter

/**
 * 스프링 시큐리티가 CSRF 방어와 보안 헤더를 맡는다.
 *
 * 로그인 여부 판단은 [ai.antop.supports.web.AdminAuthInterceptor] 가 세션으로 하므로
 * 접근 제어(authorize)는 걸지 않고 전부 통과시킨다.
 *
 * CSP 는 화면에 따라 다르므로(관리자만 인라인 스크립트 허용) 체인을 관리자용과
 * 사용자용으로 나눈다. 나머지 헤더(X-Content-Type-Options, X-Frame-Options)는
 * 시큐리티 기본값이 그대로 우리가 쓰던 값이라 따로 지정하지 않는다.
 */
@Configuration
@EnableWebSecurity
class SecurityConfig {
    /** 의견/관리자 비밀번호 단방향 해시(BCrypt). */
    @Bean
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    @Bean
    @Order(1)
    fun adminSecurityFilterChain(
        http: HttpSecurity,
        csrfAccessDeniedHandler: CsrfAccessDeniedHandler,
    ): SecurityFilterChain =
        http
            .securityMatcher("/admin/**")
            .applyCommonRules(ADMIN_CSP, csrfAccessDeniedHandler)
            // 관리 화면이 어디선가 링크되더라도 색인되지 않게 한다.
            // robots.txt 는 크롤링만 막을 뿐, 외부 링크로 알려진 주소의 색인은 막지 못한다.
            .headers { it.addHeaderWriter(StaticHeadersWriter("X-Robots-Tag", "noindex, nofollow")) }
            .build()

    @Bean
    @Order(2)
    fun userSecurityFilterChain(
        http: HttpSecurity,
        csrfAccessDeniedHandler: CsrfAccessDeniedHandler,
    ): SecurityFilterChain = http.applyCommonRules(USER_CSP, csrfAccessDeniedHandler).build()

    /** 두 체인이 CSP 만 다르고 나머지는 같으므로 공통 부분을 모아 둔다. */
    private fun HttpSecurity.applyCommonRules(
        csp: String,
        csrfAccessDeniedHandler: CsrfAccessDeniedHandler,
    ): HttpSecurity =
        authorizeHttpRequests { it.anyRequest().permitAll() }
            // CSRF 는 기본값(세션 토큰 + POST/PUT/PATCH/DELETE 검증)을 그대로 쓴다.
            .headers { headers ->
                headers
                    .contentSecurityPolicy { it.policyDirectives(csp) }
                    .referrerPolicy { it.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN) }
            }
            // 로그아웃은 /admin/logout 에서 직접 처리한다.
            .logout { it.disable() }
            .exceptionHandling { it.accessDeniedHandler(csrfAccessDeniedHandler) }

    companion object {
        /**
         * 인라인 style 속성(style="...")은 화면 곳곳에서 쓰므로 style-src 만 'unsafe-inline' 을 허용한다.
         * 본문 HTML 에 붙여넣기로 들어온 외부 이미지가 깨지지 않도록 img-src 는 https 를 허용한다.
         */
        private const val COMMON_CSP =
            "default-src 'self'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: https:; " +
                "font-src 'self'; " +
                "connect-src 'self'; " +
                "object-src 'none'; " +
                "base-uri 'none'; " +
                "form-action 'self'; " +
                "frame-ancestors 'none'"

        /** 사용자 페이지는 인라인 스크립트를 쓰지 않으므로 script-src 를 'self' 로 묶는다. */
        private const val USER_CSP = "$COMMON_CSP; script-src 'self'"

        /** 관리자 페이지는 Tailwind 설정 등 인라인 스크립트가 남아 있어 'unsafe-inline' 을 허용한다. */
        private const val ADMIN_CSP = "$COMMON_CSP; script-src 'self' 'unsafe-inline'"
    }
}

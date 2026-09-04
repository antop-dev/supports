package ai.antop.supports

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
// 스프링 시큐리티는 CSRF 방어만 쓰고 로그인은 AdminAuthInterceptor 가 처리한다.
// 기본 사용자(임의 생성 비밀번호)는 쓰지 않으므로 자동 설정을 뺀다.
@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
class SupportsApplication

fun main(args: Array<String>) {
    runApplication<SupportsApplication>(*args)
}

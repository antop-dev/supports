package ai.antop.supports.config

import com.slack.api.Slack
import com.slack.api.SlackConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * slack-api-client 의 [Slack] 클라이언트를 스프링 빈으로 등록한다.
 * 슬랙이 응답하지 않을 때 알림 스레드가 묶이지 않도록 타임아웃을 짧게 둔다.
 */
@Configuration
class SlackClientConfig {
    @Bean(destroyMethod = "close")
    fun slack(): Slack {
        val config =
            SlackConfig().apply {
                httpClientCallTimeoutMillis = 10_000
                httpClientReadTimeoutMillis = 5_000
                httpClientWriteTimeoutMillis = 5_000
            }
        return Slack.getInstance(config)
    }
}

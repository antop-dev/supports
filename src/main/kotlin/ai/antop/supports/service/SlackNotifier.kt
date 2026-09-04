package ai.antop.supports.service

import ai.antop.supports.config.AppProperties
import com.slack.api.Slack
import com.slack.api.model.kotlin_extension.block.withBlocks
import com.slack.api.webhook.Payload
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

/**
 * 의견이 접수되면 슬랙 Incoming Webhook 으로 알린다.
 * webhook URL 이 설정되지 않으면(로컬 등) 조용히 건너뛴다.
 */
@Service
class SlackNotifier(
    private val appProperties: AppProperties,
    private val slack: Slack,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 새 의견 알림을 보낸다. 알림은 부가 기능이므로 실패해도 접수 흐름에 영향을 주지 않는다.
     */
    @Async("slackExecutor")
    fun notifyFeedbackCreated(
        projectName: String,
        title: String,
        adminUrl: String,
    ) {
        val webhookUrl = appProperties.slack.webhookUrl
        if (webhookUrl.isBlank()) {
            log.debug("Slack webhook is not configured, skipping notification.")
            return
        }

        val message =
            buildString {
                append("새 의견이 등록되었습니다.\n")
                append("- 프로젝트 : ").append(escape(projectName)).append('\n')
                append("- 제목 : ").append(escape(title)).append('\n')
                append("- ").append(adminUrl)
            }
        val payload =
            Payload
                .builder()
                // 알림(푸시)이나 Block Kit 미지원 환경에서 보이는 대체 문구
                .text(message)
                .blocks(
                    withBlocks {
                        section { markdownText(message) }
                    },
                ).build()

        try {
            val response = slack.send(webhookUrl, payload)
            if (response.code == 200) {
                log.info("Slack notification sent: url={}", adminUrl)
            } else {
                log.error("Slack notification rejected: status={}, body={}, url={}", response.code, response.body, adminUrl)
            }
        } catch (ex: Exception) {
            log.error("Failed to send Slack notification: url={}", adminUrl, ex)
        }
    }

    /** 슬랙 메시지에서 특별한 의미를 갖는 문자를 이스케이프한다. */
    private fun escape(text: String): String =
        text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
}

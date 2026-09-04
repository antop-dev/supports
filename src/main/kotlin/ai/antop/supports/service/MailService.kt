package ai.antop.supports.service

import ai.antop.supports.config.AppProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.mail.autoconfigure.MailProperties
import org.springframework.context.MessageSource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.thymeleaf.context.Context
import org.thymeleaf.spring6.SpringTemplateEngine
import java.nio.charset.StandardCharsets

/**
 * 처리 완료 메일을 비동기로 발송한다. 메일 설정이 없으면 조용히 건너뛴다.
 * 본문은 Thymeleaf 템플릿(templates/mail/feedback-done.html)으로 만든다.
 *
 * SMTP 접속 계정(`spring.mail.username`)과 받는 사람에게 보이는 발신 주소
 * (`app.mail.from`)는 다르게 둘 수 있다.
 */
@Service
class MailService(
    private val mailSenderProvider: ObjectProvider<JavaMailSender>,
    private val messageSource: MessageSource,
    private val templateEngine: SpringTemplateEngine,
    private val mailProperties: MailProperties,
    private val appProperties: AppProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 관리자가 완료 처리하면서 발송을 선택했을 때, 처리 내용을 접수자에게 보낸다. */
    @Async("mailExecutor")
    fun sendDoneMail(mail: DoneMail) {
        val mailSender = mailSenderProvider.ifAvailable
        val account = mailProperties.username
        if (mailSender == null || mailProperties.host.isNullOrBlank() || account.isNullOrBlank()) {
            log.warn("Mail is not configured, skipping done mail. receiptNo={}", mail.receiptNo)
            return
        }
        // 표시용 발신 주소를 따로 지정하지 않았으면 SMTP 계정을 그대로 쓴다.
        val from = appProperties.mail.from.ifBlank { account }

        try {
            val subject = messageSource.getMessage("mail.done.subject", arrayOf(mail.receiptNo), mail.locale)
            val message = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, false, StandardCharsets.UTF_8.name())
            val fromName = appProperties.mail.fromName
            if (fromName.isBlank()) helper.setFrom(from) else helper.setFrom(from, fromName)
            val replyTo = appProperties.mail.replyTo
            if (replyTo.isNotBlank()) helper.setReplyTo(replyTo)
            helper.setTo(mail.toEmail)
            helper.setSubject(subject)
            helper.setText(render(mail), true)
            mailSender.send(message)
            log.info("Done mail sent: from={}, to={}, receiptNo={}", from, mail.toEmail, mail.receiptNo)
        } catch (ex: Exception) {
            log.error("Failed to send done mail: to={}, receiptNo={}", mail.toEmail, mail.receiptNo, ex)
        }
    }

    /** 메일 본문 HTML 을 만든다. 접수 당시 언어로 문구를 고른다. */
    private fun render(mail: DoneMail): String {
        val context =
            Context(mail.locale).apply {
                setVariable("receiptNo", mail.receiptNo)
                setVariable("projectName", mail.projectName)
                setVariable("title", mail.title)
                setVariable("replyContent", mail.replyContent)
                setVariable("viewUrl", mail.viewUrl)
            }
        return templateEngine.process("mail/feedback-done", context)
    }
}

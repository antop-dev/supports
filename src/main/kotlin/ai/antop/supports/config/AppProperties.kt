package ai.antop.supports.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 애플리케이션 전용 설정. 시크릿(슬랙 웹훅 URL 등)은 환경 변수로 주입한다.
 * SMTP 접속 정보는 스프링 표준 `spring.mail.*` 을 그대로 쓴다.
 * 관리자 계정은 설정이 아니라 admin_users 테이블에서만 관리한다.
 */
@ConfigurationProperties("app")
data class AppProperties(
    val upload: Upload = Upload(),
    val captcha: Captcha = Captcha(),
    val slack: Slack = Slack(),
    val mail: Mail = Mail(),
    /** 완료 화면 등에서 절대 URL이 필요할 때 사용. 비어 있으면 요청 기준으로 생성한다. */
    val baseUrl: String = "",
) {
    data class Upload(
        /** 업로드 파일 저장 루트 디렉터리 */
        val dir: String = "./data/uploads",
        /** 의견당 최대 첨부 파일 수 */
        val maxFiles: Int = 10,
        /** 청크 업로드로 받는 파일 하나의 최대 크기(바이트). 기본 10MB. */
        val maxFileSizeBytes: Long = 10 * 1024 * 1024L,
        /** 청크 하나의 최대 크기(바이트). 이보다 큰 청크는 거부한다. 기본 8MB(클라이언트 청크 크기보다 여유를 둔다). */
        val chunkSizeBytes: Long = 8 * 1024 * 1024L,
        /** 폼 제출로 이어지지 않고 방치된 임시 업로드를 이 시간(시간 단위) 이후 정리한다. */
        val pendingTtlHours: Long = 24,
    )

    data class Mail(
        /**
         * SMTP 연결 암호화 방식. JavaMail 의 `ssl.enable` / `starttls.enable` / `starttls.required`
         * 세 속성이 이 값 하나에서 함께 결정된다([ai.antop.supports.config.MailConfig]).
         * 따로 두면 `ssl=true` 인데 `starttls=true` 같은 모순된 조합이 만들어질 수 있다.
         */
        val tls: Tls = Tls.STARTTLS,
        /**
         * 받는 사람에게 보이는 발신 주소. 비어 있으면 `spring.mail.username`(SMTP 계정)을 쓴다.
         * SMTP 서버가 소유를 인증한 도메인의 주소여야 한다. 그렇지 않으면 서버가
         * From 을 계정 주소로 덮어쓰거나 발송을 거부한다(RESEND.md 참고).
         */
        val from: String = "",
        /** 발신자 표시 이름. 비어 있으면 주소만 보인다. */
        val fromName: String = "",
        /** 회신 주소. 비어 있으면 회신은 발신 주소로 간다. */
        val replyTo: String = "",
    ) {
        enum class Tls(
            /** 처음부터 TLS 로 연결한다(SMTPS). */
            val implicitSsl: Boolean,
            /** 평문으로 붙은 뒤 TLS 로 승격한다. */
            val startTls: Boolean,
        ) {
            /** 587 표준. 승격에 실패하면 발송하지 않는다. */
            STARTTLS(implicitSsl = false, startTls = true),

            /** 465 암시적 TLS. */
            SSL(implicitSsl = true, startTls = false),

            /** 암호화 없음(평문). STARTTLS 를 지원하지 않는 로컬 더미 SMTP 전용. */
            NONE(implicitSsl = false, startTls = false),
        }
    }

    data class Slack(
        /** 슬랙 Incoming Webhook URL. 비어 있으면 알림을 보내지 않는다. */
        val webhookUrl: String = "",
    )

    data class Captcha(
        /** false 면 캡차 검증을 통과시킨다(자동화 테스트용). 운영에서는 true 를 유지한다. */
        val enabled: Boolean = true,
        val length: Int = 6,
        val width: Int = 180,
        val height: Int = 60,
    )
}

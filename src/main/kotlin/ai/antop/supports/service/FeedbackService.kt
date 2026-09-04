package ai.antop.supports.service

import ai.antop.supports.common.DateTimes
import ai.antop.supports.common.HtmlSanitizer
import ai.antop.supports.config.LocaleConfig
import ai.antop.supports.domain.Feedback
import ai.antop.supports.domain.FeedbackFile
import ai.antop.supports.domain.FeedbackStatus
import ai.antop.supports.dto.FeedbackDetail
import ai.antop.supports.dto.FeedbackForm
import ai.antop.supports.dto.FeedbackListItem
import ai.antop.supports.dto.PublicFeedbackListItem
import ai.antop.supports.exception.NotFoundException
import ai.antop.supports.repository.FeedbackRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import java.security.SecureRandom
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Service
@Transactional(readOnly = true)
class FeedbackService(
    private val feedbackRepository: FeedbackRepository,
    private val projectService: ProjectService,
    private val fileStorageService: FileStorageService,
    private val passwordEncoder: PasswordEncoder,
    private val mailService: MailService,
) {
    private val random = SecureRandom()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC)

    data class DownloadInfo(
        val feedbackId: String,
        val storedName: String,
        val originalName: String,
        val contentType: String?,
        val secret: Boolean,
    )

    @Transactional
    fun create(
        form: FeedbackForm,
        fileUploadIds: List<String>,
        locale: Locale,
    ): Feedback {
        val project = projectService.getById(form.projectId)
        require(project.enabled) { "선택할 수 없는 프로젝트입니다." }

        val passwordHash = form.password?.takeIf { it.isNotBlank() }?.let { passwordEncoder.encode(it) }
        val email = form.email?.takeIf { it.isNotBlank() }?.trim()

        val feedback =
            Feedback(
                receiptNo = generateReceiptNo(),
                project = project,
                title = form.title.trim(),
                email = email,
                // 이메일을 적지 않았으면 보낼 곳이 없으므로 수신 여부도 함께 꺼둔다.
                emailReply = email != null && form.emailReply,
                content = HtmlSanitizer.sanitize(form.content),
                passwordHash = passwordHash,
                // 처리 완료 메일을 접수자가 쓰던 언어로 보내려고 남겨 둔다.
                locale = locale.language,
            )

        // 청크 업로드로 미리 받아둔 임시 파일을 이 의견의 첨부로 확정한다.
        fileUploadIds.forEach { uploadId ->
            val stored = fileStorageService.claim(feedback.id, uploadId)
            feedback.addFile(
                FeedbackFile(
                    originalName = stored.originalName,
                    storedName = stored.storedName,
                    contentType = stored.contentType,
                    size = stored.size,
                ),
            )
        }

        return feedbackRepository.save(feedback)
    }

    fun getDetail(id: String): FeedbackDetail = FeedbackDetail.from(loadDetail(id))

    /** 접수번호로 의견 id를 찾는다. 없으면 null. */
    fun findIdByReceiptNo(receiptNo: String): String? = feedbackRepository.findByReceiptNo(receiptNo.trim())?.id

    /** 접수 확인 페이지 하단에 보여줄, 비밀글이 아닌 최근 의견 10건. */
    fun listRecentPublic(): List<PublicFeedbackListItem> =
        feedbackRepository.findTop10ByPasswordHashIsNullOrderByCreatedAtDesc().map(PublicFeedbackListItem::from)

    fun isSecret(id: String): Boolean = loadDetail(id).isSecret

    fun verifyPassword(
        id: String,
        rawPassword: String,
    ): Boolean {
        val hash = loadDetail(id).passwordHash ?: return true
        return passwordEncoder.matches(rawPassword, hash)
    }

    fun pageList(
        receiptNo: String?,
        title: String?,
        email: String?,
        status: FeedbackStatus?,
        createdDate: LocalDate?,
        pageable: Pageable,
    ): Page<FeedbackListItem> {
        // 등록일은 서버 기본 시간대의 하루(00:00 이상 ~ 다음 날 00:00 미만) 범위로 변환한다.
        // 관리자 화면이 KST 로 보여주므로 날짜 검색도 KST 하루 단위로 자른다.
        val zone = DateTimes.KST
        val createdFrom = createdDate?.atStartOfDay(zone)?.toInstant()
        val createdTo = createdDate?.plusDays(1)?.atStartOfDay(zone)?.toInstant()
        return feedbackRepository
            .search(
                status = status,
                receiptNo = receiptNo?.trim()?.takeIf { it.isNotBlank() },
                title = title?.trim()?.takeIf { it.isNotBlank() },
                email = email?.trim()?.takeIf { it.isNotBlank() },
                createdFrom = createdFrom,
                createdTo = createdTo,
                pageable = pageable,
            ).map(FeedbackListItem::from)
    }

    fun getFileForDownload(
        feedbackId: String,
        fileId: String,
    ): DownloadInfo {
        val feedback = loadDetail(feedbackId)
        val file =
            feedback.files.firstOrNull { it.id == fileId }
                ?: throw NotFoundException("file not found: $fileId")
        return DownloadInfo(
            feedbackId = feedbackId,
            storedName = file.storedName,
            originalName = file.originalName,
            contentType = file.contentType,
            secret = feedback.isSecret,
        )
    }

    @Transactional
    fun saveReply(
        id: String,
        content: String,
    ) {
        applyReply(loadDetail(id), content)
    }

    /**
     * 처리 내용을 저장하고 완료 / 완료 취소로 전환한다.
     * 완료로 바뀌었고 접수자가 이메일 수신을 원했으면 처리 결과 메일을 보낸다(완료 취소에는 보내지 않는다).
     */
    @Transactional
    fun completeWithReply(
        id: String,
        content: String,
        viewUrl: String,
    ): Feedback {
        val feedback = loadDetail(id)
        applyReply(feedback, content)
        if (feedback.status == FeedbackStatus.DONE) {
            feedback.markReceived()
        } else {
            feedback.markDone()
            // 발송 여부는 관리자가 고르지 않는다. 접수 당시 접수자가 고른 수신 여부를 그대로 따른다.
            // emailReply 는 이메일을 적었을 때만 켜지므로(접수 처리 참고) 보낼 곳이 없는 경우는 걸러진다.
            if (feedback.emailReply) {
                sendDoneMailAfterCommit(feedback, viewUrl)
            }
        }
        return feedback
    }

    private fun applyReply(
        feedback: Feedback,
        content: String,
    ) {
        feedback.replyContent = if (content.isBlank()) null else HtmlSanitizer.sanitize(content)
    }

    /**
     * 커밋된 뒤에 메일을 보낸다. 트랜잭션이 롤백되면 완료 처리가 없던 일이 되므로 메일도 나가지 않는다.
     * 발송에 필요한 값은 미리 꺼내 둔다(다른 스레드에서 준영속 엔티티를 건드리지 않도록).
     */
    private fun sendDoneMailAfterCommit(
        feedback: Feedback,
        viewUrl: String,
    ) {
        val toEmail = feedback.email?.takeIf { it.isNotBlank() } ?: return
        val mail =
            DoneMail(
                toEmail = toEmail,
                receiptNo = feedback.receiptNo,
                projectName = feedback.project.name,
                title = feedback.title,
                replyContent = feedback.replyContent,
                viewUrl = viewUrl,
                // 관리자가 아니라 접수자가 쓰던 언어로 보낸다.
                locale = LocaleConfig.localeOf(feedback.locale),
            )
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    mailService.sendDoneMail(mail)
                }
            },
        )
    }

    private fun loadDetail(id: String): Feedback =
        feedbackRepository.findWithDetailById(id) ?: throw NotFoundException("feedback not found: $id")

    private fun generateReceiptNo(): String {
        val datePart = dateFormatter.format(java.time.Instant.now())
        repeat(10) {
            val suffix = (1..6).joinToString("") { RECEIPT_CHARS[random.nextInt(RECEIPT_CHARS.length)].toString() }
            val candidate = "$datePart-$suffix"
            if (!feedbackRepository.existsByReceiptNo(candidate)) {
                return candidate
            }
        }
        error("접수번호 생성에 실패했습니다.")
    }

    companion object {
        // 혼동되기 쉬운 문자(0/O, 1/I) 제외
        private const val RECEIPT_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}

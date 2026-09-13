package ai.antop.supports.dto

import ai.antop.supports.common.FileKind
import ai.antop.supports.common.HtmlSanitizer
import ai.antop.supports.domain.AdminUser
import ai.antop.supports.domain.Feedback
import ai.antop.supports.domain.FeedbackFile
import ai.antop.supports.domain.FeedbackStatus
import ai.antop.supports.domain.Project
import java.time.Instant

/**
 * 관리자 로그인(AJAX) 응답.
 *
 * errors 는 입력 항목별 오류(항목명 → 메시지), message 는 항목에 매이지 않는 전역 오류다.
 * 캡차는 한 번 검증하면 세션에서 지워지므로, 실패 응답을 받은 화면은 캡차 이미지를 다시 받아야 한다.
 */
data class LoginResponse(
    val ok: Boolean,
    val message: String? = null,
    val errors: Map<String, String> = emptyMap(),
    /** 로그인 성공 후 이동할 주소. 로그인 전에 들어오려던 화면이 있으면 그 주소가 담긴다. */
    val redirect: String? = null,
)

/** 관리자 프로젝트 목록 항목. */
data class ProjectListItem(
    val id: String,
    val name: String,
    val code: String,
    val sortOrder: Int,
    val enabled: Boolean,
    val url: String?,
    val createdAt: Instant,
) {
    companion object {
        fun from(project: Project): ProjectListItem =
            ProjectListItem(
                id = project.id,
                name = project.name,
                code = project.code,
                sortOrder = project.sortOrder,
                enabled = project.enabled,
                url = project.url,
                createdAt = project.createdAt,
            )
    }
}

/** 관리자 계정 목록 항목. 비밀번호 해시는 노출하지 않는다. */
data class AdminUserItem(
    val id: String,
    val username: String,
    val createdAt: Instant,
) {
    companion object {
        fun from(user: AdminUser): AdminUserItem =
            AdminUserItem(
                id = user.id,
                username = user.username,
                createdAt = user.createdAt,
            )
    }
}

/** 첨부 파일 응답. */
data class FileDto(
    val id: String,
    val originalName: String,
    val size: Long,
    val contentType: String?,
    val kind: FileKind,
) {
    val isImage: Boolean get() = kind == FileKind.IMAGE
    val isPdf: Boolean get() = kind == FileKind.PDF

    companion object {
        fun from(file: FeedbackFile): FileDto =
            FileDto(
                id = file.id,
                originalName = file.originalName,
                size = file.size,
                contentType = file.contentType,
                kind = FileKind.from(file.contentType),
            )
    }
}

/** 관리자 목록 항목. */
data class FeedbackListItem(
    val id: String,
    val receiptNo: String,
    val projectName: String,
    val title: String,
    val email: String?,
    val status: FeedbackStatus,
    val secret: Boolean,
    val createdAt: Instant,
    val processedAt: Instant?,
) {
    companion object {
        fun from(feedback: Feedback): FeedbackListItem =
            FeedbackListItem(
                id = feedback.id,
                receiptNo = feedback.receiptNo,
                projectName = feedback.project.name,
                title = feedback.title,
                email = feedback.email,
                status = feedback.status,
                secret = feedback.isSecret,
                createdAt = feedback.createdAt,
                processedAt = feedback.processedAt,
            )
    }
}

/** 접수 확인 페이지 하단의 공개 의견 목록 항목. */
data class PublicFeedbackListItem(
    val id: String,
    val receiptNo: String,
    val title: String,
    val status: FeedbackStatus,
    val createdAt: Instant,
) {
    val done: Boolean
        get() = status == FeedbackStatus.DONE

    companion object {
        fun from(feedback: Feedback): PublicFeedbackListItem =
            PublicFeedbackListItem(
                id = feedback.id,
                receiptNo = feedback.receiptNo,
                title = feedback.title,
                status = feedback.status,
                createdAt = feedback.createdAt,
            )
    }
}

/**
 * 의견 상세(관리자/사용자 공용 본문).
 *
 * content/replyContent 는 화면에서 th:utext 로 그대로 출력되므로,
 * 저장할 때뿐 아니라 내보낼 때도 한 번 더 새니타이징한다.
 * (새니타이저가 없던 시절의 옛 데이터나 DB 를 직접 고친 값까지 막기 위한 이중 방어)
 */
data class FeedbackDetail(
    val id: String,
    val receiptNo: String,
    val projectName: String,
    val title: String,
    val email: String?,
    /** 접수자가 처리 결과를 이메일로 받겠다고 했는지. */
    val emailReply: Boolean,
    val content: String,
    val status: FeedbackStatus,
    val replyContent: String?,
    val secret: Boolean,
    val createdAt: Instant,
    val processedAt: Instant?,
    val files: List<FileDto>,
) {
    val done: Boolean
        get() = status == FeedbackStatus.DONE

    companion object {
        fun from(feedback: Feedback): FeedbackDetail =
            FeedbackDetail(
                id = feedback.id,
                receiptNo = feedback.receiptNo,
                projectName = feedback.project.name,
                title = feedback.title,
                email = feedback.email,
                emailReply = feedback.emailReply,
                content = HtmlSanitizer.sanitize(feedback.content),
                status = feedback.status,
                replyContent = feedback.replyContent?.let(HtmlSanitizer::sanitize),
                secret = feedback.isSecret,
                createdAt = feedback.createdAt,
                processedAt = feedback.processedAt,
                files = feedback.files.map(FileDto::from),
            )
    }
}

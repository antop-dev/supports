package ai.antop.supports.domain

import ai.antop.supports.common.IdGenerator
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.Instant

/** 접수된 의견. */
@Entity
@Table(name = "feedbacks")
class Feedback(
    @Column(name = "receipt_no", nullable = false, unique = true, updatable = false)
    val receiptNo: String,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    var project: Project,
    @Column(name = "title", nullable = false)
    var title: String,
    @Column(name = "email")
    var email: String? = null,
    /** 접수자가 처리 결과를 이메일로 받기를 원하는지. 관리자 화면에서 메일 발송 여부의 기본값이 된다. */
    @Column(name = "email_reply", nullable = false)
    var emailReply: Boolean = false,
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String,
    /** BCrypt 해시. null 이면 공개 의견. */
    @Column(name = "password_hash")
    var passwordHash: String? = null,
    /** 접수 당시 사용자의 언어(ko/en/ja/zh). 처리 완료 메일을 이 언어로 보낸다. */
    @Column(name = "locale", nullable = false, updatable = false)
    val locale: String = "ko",
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: FeedbackStatus = FeedbackStatus.RECEIVED,
    @Column(name = "reply_content", columnDefinition = "TEXT")
    var replyContent: String? = null,
    @Column(name = "processed_at")
    var processedAt: Instant? = null,
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: String = IdGenerator.newId(),
) : BaseTimeEntity() {
    @OneToMany(
        mappedBy = "feedback",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY,
    )
    val files: MutableList<FeedbackFile> = mutableListOf()

    val isSecret: Boolean
        get() = passwordHash != null

    fun addFile(file: FeedbackFile) {
        file.feedback = this
        files += file
    }

    fun markDone() {
        status = FeedbackStatus.DONE
        processedAt = Instant.now()
    }

    fun markReceived() {
        status = FeedbackStatus.RECEIVED
        processedAt = null
    }
}

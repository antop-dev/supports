package ai.antop.supports.domain

import ai.antop.supports.common.IdGenerator
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/** 의견에 첨부된 파일 메타데이터. 실제 파일은 파일시스템에 저장한다. */
@Entity
@Table(name = "feedback_files")
class FeedbackFile(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feedback_id", nullable = false)
    var feedback: Feedback? = null,
    @Column(name = "original_name", nullable = false)
    val originalName: String,
    @Column(name = "stored_name", nullable = false)
    val storedName: String,
    @Column(name = "content_type")
    val contentType: String? = null,
    @Column(name = "size", nullable = false)
    val size: Long,
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: String = IdGenerator.newId(),
) : BaseTimeEntity()

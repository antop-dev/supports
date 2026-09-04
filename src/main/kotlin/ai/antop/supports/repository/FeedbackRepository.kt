package ai.antop.supports.repository

import ai.antop.supports.domain.Feedback
import ai.antop.supports.domain.FeedbackStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface FeedbackRepository : JpaRepository<Feedback, String> {
    fun existsByReceiptNo(receiptNo: String): Boolean

    fun findByReceiptNo(receiptNo: String): Feedback?

    fun countByProjectId(projectId: String): Long

    /** 접수 확인 페이지 하단에 보여줄, 비밀글이 아닌 최근 의견 목록. */
    fun findTop10ByPasswordHashIsNullOrderByCreatedAtDesc(): List<Feedback>

    /** 접수번호/제목/이메일/처리상태/등록일 조건(널이면 무시)으로 검색한다. */
    @EntityGraph(attributePaths = ["project"])
    @Query(
        """
        SELECT f FROM Feedback f
        WHERE (:status IS NULL OR f.status = :status)
          AND (:receiptNo IS NULL OR LOWER(f.receiptNo) LIKE LOWER(CONCAT('%', :receiptNo, '%')))
          AND (:title IS NULL OR LOWER(f.title) LIKE LOWER(CONCAT('%', :title, '%')))
          AND (:email IS NULL OR LOWER(f.email) LIKE LOWER(CONCAT('%', :email, '%')))
          AND (:createdFrom IS NULL OR f.createdAt >= :createdFrom)
          AND (:createdTo IS NULL OR f.createdAt < :createdTo)
        ORDER BY f.createdAt DESC
        """,
    )
    fun search(
        @Param("status") status: FeedbackStatus?,
        @Param("receiptNo") receiptNo: String?,
        @Param("title") title: String?,
        @Param("email") email: String?,
        @Param("createdFrom") createdFrom: Instant?,
        @Param("createdTo") createdTo: Instant?,
        pageable: Pageable,
    ): Page<Feedback>

    @EntityGraph(attributePaths = ["project", "files"])
    fun findWithDetailById(id: String): Feedback?
}

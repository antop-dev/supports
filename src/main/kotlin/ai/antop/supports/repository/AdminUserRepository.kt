package ai.antop.supports.repository

import ai.antop.supports.domain.AdminUser
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AdminUserRepository : JpaRepository<AdminUser, String> {
    fun findByUsername(username: String): AdminUser?

    fun existsByUsername(username: String): Boolean

    /** 아이디 조건(널이면 무시)으로 검색한다. */
    @Query(
        """
        SELECT a FROM AdminUser a
        WHERE (:username IS NULL OR LOWER(a.username) LIKE LOWER(CONCAT('%', :username, '%')))
        ORDER BY a.createdAt ASC
        """,
    )
    fun search(
        @Param("username") username: String?,
        pageable: Pageable,
    ): Page<AdminUser>
}

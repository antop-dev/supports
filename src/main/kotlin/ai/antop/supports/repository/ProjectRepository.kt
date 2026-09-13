package ai.antop.supports.repository

import ai.antop.supports.domain.Project
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProjectRepository : JpaRepository<Project, String> {
    fun findAllByEnabledTrueOrderBySortOrderAscCreatedAtAsc(): List<Project>

    fun findByCode(code: String): Project?

    /** 프로젝트명/활성화 여부 조건(널이면 무시)으로 검색한다. */
    @Query(
        """
        SELECT p FROM Project p
        WHERE (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')))
          AND (:enabled IS NULL OR p.enabled = :enabled)
        ORDER BY p.sortOrder ASC, p.createdAt ASC
        """,
    )
    fun search(
        @Param("name") name: String?,
        @Param("enabled") enabled: Boolean?,
        pageable: Pageable,
    ): Page<Project>
}

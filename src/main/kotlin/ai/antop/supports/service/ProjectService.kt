package ai.antop.supports.service

import ai.antop.supports.domain.Project
import ai.antop.supports.dto.ProjectListItem
import ai.antop.supports.exception.NotFoundException
import ai.antop.supports.repository.FeedbackRepository
import ai.antop.supports.repository.ProjectRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ProjectService(
    private val projectRepository: ProjectRepository,
    private val feedbackRepository: FeedbackRepository,
) {
    fun listEnabled(): List<Project> = projectRepository.findAllByEnabledTrueOrderBySortOrderAscCreatedAtAsc()

    fun getById(id: String): Project = projectRepository.findByIdOrNull(id) ?: throw NotFoundException("project not found: $id")

    fun pageList(
        name: String?,
        enabled: Boolean?,
        pageable: Pageable,
    ): Page<ProjectListItem> =
        projectRepository
            .search(
                name = name?.trim()?.takeIf { it.isNotBlank() },
                enabled = enabled,
                pageable = pageable,
            ).map(ProjectListItem::from)

    @Transactional
    fun create(
        name: String,
        enabled: Boolean,
        sortOrder: Int,
        url: String? = null,
    ): Project =
        projectRepository.save(
            Project(name = name.trim(), enabled = enabled, sortOrder = sortOrder, url = normalizeUrl(url)),
        )

    /** 목록에서 셀 하나만 수정한다. 잘못된 값이면 IllegalArgumentException. */
    @Transactional
    fun updateField(
        id: String,
        field: String,
        value: String,
    ): ProjectListItem {
        val project = getById(id)
        when (field) {
            "name" -> {
                val name = value.trim()
                require(name.isNotBlank()) { "프로젝트명을 입력하세요." }
                require(name.length <= MAX_NAME_LENGTH) { "프로젝트명은 최대 ${MAX_NAME_LENGTH}자까지 입력할 수 있습니다." }
                project.name = name
            }

            "sortOrder" -> {
                val sortOrder = value.trim().toIntOrNull()
                requireNotNull(sortOrder) { "정렬 순서는 숫자로 입력하세요." }
                project.sortOrder = sortOrder
            }

            "enabled" -> project.enabled = value.trim().equals("true", ignoreCase = true)

            "url" -> project.url = normalizeUrl(value)

            else -> throw IllegalArgumentException("수정할 수 없는 항목입니다: $field")
        }
        return ProjectListItem.from(project)
    }

    @Transactional
    fun delete(id: String) {
        val project = getById(id)
        require(feedbackRepository.countByProjectId(id) == 0L) {
            "이미 접수된 의견이 있는 프로젝트는 삭제할 수 없습니다. 비활성화하세요."
        }
        projectRepository.delete(project)
    }

    /** 빈 값은 null 로, 값이 있으면 http(s) URL 형식인지 검증하고 앞뒤 공백을 제거한다. */
    private fun normalizeUrl(url: String?): String? {
        val trimmed = url?.trim()?.takeIf { it.isNotBlank() } ?: return null
        require(trimmed.length <= MAX_URL_LENGTH) { "URL은 최대 ${MAX_URL_LENGTH}자까지 입력할 수 있습니다." }
        require(URL_PATTERN.matches(trimmed)) { "URL은 http:// 또는 https:// 로 시작해야 합니다." }
        return trimmed
    }

    companion object {
        private const val MAX_NAME_LENGTH = 100
        private const val MAX_URL_LENGTH = 500
        private val URL_PATTERN = Regex("^https?://.+")
    }
}

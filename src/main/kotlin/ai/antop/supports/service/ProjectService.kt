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

    /** 공개 코드로 활성 프로젝트를 찾는다. 없거나 비활성이면 null. */
    fun findEnabledByCode(code: String?): Project? {
        val normalized = code?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: return null
        return projectRepository.findByCode(normalized)?.takeIf { it.enabled }
    }

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
        code: String? = null,
    ): Project {
        val trimmedName = name.trim()
        return projectRepository.save(
            Project(
                name = trimmedName,
                code = resolveCode(code, trimmedName, currentId = null),
                enabled = enabled,
                sortOrder = sortOrder,
                url = normalizeUrl(url),
            ),
        )
    }

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

            "code" -> project.code = resolveCode(value, project.name, currentId = project.id)

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

    /**
     * 저장할 공개 코드를 정한다. 입력값이 있으면 형식을 검증해서 쓰고, 비어 있으면 프로젝트명에서 만든다.
     * 이미 쓰이는 코드면 뒤에 -2, -3 을 붙여 비켜간다.
     */
    private fun resolveCode(
        input: String?,
        name: String,
        currentId: String?,
    ): String {
        val requested = input?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
        if (requested != null) {
            require(requested.length <= MAX_CODE_LENGTH) { "코드는 최대 ${MAX_CODE_LENGTH}자까지 입력할 수 있습니다." }
            require(CODE_PATTERN.matches(requested)) { "코드는 영문 소문자·숫자·하이픈(-)만 쓸 수 있습니다." }
            require(isCodeFree(requested, currentId)) { "이미 사용 중인 코드입니다: $requested" }
            return requested
        }
        val base = slugify(name).ifBlank { DEFAULT_CODE_BASE }
        if (isCodeFree(base, currentId)) {
            return base
        }
        // 흔한 충돌은 몇 번 안에 끝난다. 그래도 안 되면 사용자가 직접 코드를 정하게 한다.
        val suffix = (2..CODE_SUFFIX_LIMIT).firstOrNull { isCodeFree("$base-$it", currentId) }
        requireNotNull(suffix) { "코드를 자동으로 만들지 못했습니다. 코드를 직접 입력하세요." }
        return "$base-$suffix"
    }

    private fun isCodeFree(
        code: String,
        currentId: String?,
    ): Boolean {
        val owner = projectRepository.findByCode(code) ?: return true
        return owner.id == currentId
    }

    /** 프로젝트명을 코드로 쓸 수 있는 형태로 바꾼다. 한글처럼 쓸 수 없는 글자만 있으면 빈 문자열이 된다. */
    private fun slugify(name: String): String =
        name
            .lowercase()
            .replace(NON_CODE_CHARS, "-")
            .take(MAX_CODE_LENGTH)
            .trim('-')

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
        private const val MAX_CODE_LENGTH = 50
        private const val DEFAULT_CODE_BASE = "project"
        private const val CODE_SUFFIX_LIMIT = 100
        private val URL_PATTERN = Regex("^https?://.+")
        private val CODE_PATTERN = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
        private val NON_CODE_CHARS = Regex("[^a-z0-9]+")
    }
}

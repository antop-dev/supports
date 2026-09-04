package ai.antop.supports.web.admin

import ai.antop.supports.dto.CellUpdateRequest
import ai.antop.supports.dto.ProjectForm
import ai.antop.supports.dto.ProjectListItem
import ai.antop.supports.service.ProjectService
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@RequestMapping("/admin/projects")
class AdminProjectController(
    private val projectService: ProjectService,
) {
    /** 목록 셸. 실제 데이터는 Grid.js가 /data 에서 가져온다. */
    @GetMapping
    fun list(): String = "admin/project-list"

    /** Grid.js 서버 사이드 데이터: 검색 조건 + 페이지 → { data, total } */
    @GetMapping("/data")
    @ResponseBody
    fun data(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) name: String?,
        @RequestParam(required = false) enabled: String?,
    ): Map<String, Any> {
        val enabledFlag = enabled?.takeIf { it.isNotBlank() }?.toBooleanStrictOrNull()
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 200))
        val result = projectService.pageList(name, enabledFlag, pageable)
        val rows =
            result.content.map {
                mapOf(
                    "id" to it.id,
                    "name" to it.name,
                    "sortOrder" to it.sortOrder,
                    "enabled" to it.enabled,
                    "url" to (it.url ?: ""),
                    "createdAt" to it.createdAt.toEpochMilli(),
                )
            }
        return mapOf("data" to rows, "total" to result.totalElements)
    }

    /** 팝업에서 프로젝트를 등록한다. */
    @PostMapping
    @ResponseBody
    fun create(
        @RequestBody form: ProjectForm,
    ): ResponseEntity<Map<String, Any>> =
        runCatching {
            val name = form.name.trim()
            require(name.isNotBlank()) { "프로젝트명을 입력하세요." }
            require(name.length <= MAX_NAME_LENGTH) { "프로젝트명은 최대 ${MAX_NAME_LENGTH}자까지 입력할 수 있습니다." }
            projectService.create(name, form.enabled, form.sortOrder, form.url)
        }.fold(
            onSuccess = { ResponseEntity.ok(okBody()) },
            onFailure = { ResponseEntity.badRequest().body(errorBody(it)) },
        )

    /** 목록에서 셀 하나를 즉시 저장한다. */
    @PostMapping("/{id}/cell")
    @ResponseBody
    fun updateCell(
        @PathVariable id: String,
        @RequestBody request: CellUpdateRequest,
    ): ResponseEntity<Map<String, Any>> =
        runCatching { projectService.updateField(id, request.field, request.value) }
            .fold(
                onSuccess = { ResponseEntity.ok(mapOf("ok" to true, "value" to savedValue(it, request.field))) },
                onFailure = { ResponseEntity.badRequest().body(errorBody(it)) },
            )

    @PostMapping("/{id}/delete")
    @ResponseBody
    fun delete(
        @PathVariable id: String,
    ): ResponseEntity<Map<String, Any>> =
        runCatching { projectService.delete(id) }
            .fold(
                onSuccess = { ResponseEntity.ok(okBody()) },
                onFailure = { ResponseEntity.badRequest().body(errorBody(it)) },
            )

    private fun savedValue(
        item: ProjectListItem,
        field: String,
    ): Any =
        when (field) {
            "name" -> item.name
            "sortOrder" -> item.sortOrder
            "url" -> item.url ?: ""
            else -> item.enabled
        }

    private fun okBody(): Map<String, Any> = mapOf("ok" to true)

    private fun errorBody(error: Throwable): Map<String, Any> = mapOf("ok" to false, "message" to (error.message ?: "처리하지 못했습니다."))

    companion object {
        private const val MAX_NAME_LENGTH = 100
    }
}

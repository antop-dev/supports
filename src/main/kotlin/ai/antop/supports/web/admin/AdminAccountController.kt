package ai.antop.supports.web.admin

import ai.antop.supports.dto.AdminUserForm
import ai.antop.supports.service.AdminUserService
import ai.antop.supports.web.SessionKeys
import jakarta.servlet.http.HttpSession
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
@RequestMapping("/admin/accounts")
class AdminAccountController(
    private val adminUserService: AdminUserService,
) {
    /** 목록 셸. 실제 데이터는 Grid.js가 /data 에서 가져온다. */
    @GetMapping
    fun list(): String = "admin/account-list"

    /** Grid.js 서버 사이드 데이터: 검색 조건 + 페이지 → { data, total } */
    @GetMapping("/data")
    @ResponseBody
    fun data(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) username: String?,
        session: HttpSession,
    ): Map<String, Any> {
        val current = session.getAttribute(SessionKeys.ADMIN) as? String
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 200))
        val result = adminUserService.pageList(username, pageable)
        val rows =
            result.content.map {
                mapOf(
                    "id" to it.id,
                    "username" to it.username,
                    "createdAt" to it.createdAt.toEpochMilli(),
                    "self" to (it.username == current),
                )
            }
        return mapOf("data" to rows, "total" to result.totalElements)
    }

    /** 팝업에서 관리자 계정을 등록한다. */
    @PostMapping
    @ResponseBody
    fun create(
        @RequestBody form: AdminUserForm,
    ): ResponseEntity<Map<String, Any>> =
        runCatching { adminUserService.create(form.username, form.password) }
            .fold(
                onSuccess = { ResponseEntity.ok(mapOf("ok" to true)) },
                onFailure = { ResponseEntity.badRequest().body(errorBody(it)) },
            )

    @PostMapping("/{id}/delete")
    @ResponseBody
    fun delete(
        @PathVariable id: String,
        session: HttpSession,
    ): ResponseEntity<Map<String, Any>> {
        val current = session.getAttribute(SessionKeys.ADMIN) as? String ?: ""
        return runCatching { adminUserService.delete(id, current) }
            .fold(
                onSuccess = { ResponseEntity.ok(mapOf("ok" to true)) },
                onFailure = { ResponseEntity.badRequest().body(errorBody(it)) },
            )
    }

    private fun errorBody(error: Throwable): Map<String, Any> = mapOf("ok" to false, "message" to (error.message ?: "처리하지 못했습니다."))
}

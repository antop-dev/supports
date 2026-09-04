package ai.antop.supports.web.admin

import ai.antop.supports.domain.FeedbackStatus
import ai.antop.supports.dto.ReplyForm
import ai.antop.supports.service.FeedbackService
import ai.antop.supports.web.UrlBuilder
import jakarta.servlet.http.HttpServletRequest
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import java.time.LocalDate

@Controller
@RequestMapping("/admin/feedbacks")
class AdminFeedbackController(
    private val feedbackService: FeedbackService,
    private val urlBuilder: UrlBuilder,
) {
    /** 목록 셸(탭 + 검색 + 그리드 컨테이너). 실제 데이터는 Grid.js가 /data 에서 가져온다. */
    @GetMapping
    fun list(): String = "admin/feedback-list"

    /** Grid.js 서버 사이드 데이터: 검색 조건 + 페이지 → { data, total } */
    @GetMapping("/data")
    @ResponseBody
    fun data(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) receiptNo: String?,
        @RequestParam(required = false) title: String?,
        @RequestParam(required = false) email: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) createdDate: String?,
    ): Map<String, Any> {
        val statusEnum =
            status?.takeIf { it.isNotBlank() }?.let { runCatching { FeedbackStatus.valueOf(it) }.getOrNull() }
        val created =
            createdDate?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 200))
        val result = feedbackService.pageList(receiptNo, title, email, statusEnum, created, pageable)
        val rows =
            result.content.map {
                mapOf(
                    "id" to it.id,
                    "receiptNo" to it.receiptNo,
                    "title" to it.title,
                    "email" to (it.email ?: ""),
                    "status" to it.status.name,
                    "secret" to it.secret,
                    "createdAt" to it.createdAt.toEpochMilli(),
                    "processedAt" to it.processedAt?.toEpochMilli(),
                )
            }
        return mapOf("data" to rows, "total" to result.totalElements)
    }

    /**
     * 상세는 목록 위 레이어 팝업으로 띄운다.
     * 이 주소로 직접 들어오면 목록을 그리고 해당 팝업을 자동으로 연다.
     */
    @GetMapping("/{id}")
    fun detail(
        @PathVariable id: String,
        model: Model,
    ): String {
        model.addAttribute("openId", id)
        return "admin/feedback-list"
    }

    /** 팝업에 채울 상세 내용(HTML 조각). */
    @GetMapping("/{id}/detail")
    fun detailFragment(
        @PathVariable id: String,
        model: Model,
    ): String {
        model.addAttribute("feedback", feedbackService.getDetail(id))
        return "admin/feedback-detail :: modal"
    }

    /** 팝업에서 처리 내용을 저장한다. */
    @PostMapping("/{id}/reply")
    @ResponseBody
    fun saveReply(
        @PathVariable id: String,
        @RequestBody form: ReplyForm,
    ): ResponseEntity<Map<String, Any>> =
        runCatching { feedbackService.saveReply(id, form.content) }
            .fold(
                onSuccess = { ResponseEntity.ok(statusBody(id)) },
                onFailure = { ResponseEntity.badRequest().body(errorBody(it)) },
            )

    /**
     * 팝업에서 처리 내용을 저장하고 완료/완료 취소로 전환한다.
     * 완료 메일은 접수자가 수신을 원한 경우에만 나가며, 관리자가 따로 고르지 않는다.
     * 메일 언어는 관리자가 아니라 접수 당시 저장해 둔 접수자의 언어를 따른다.
     */
    @PostMapping("/{id}/complete")
    @ResponseBody
    fun toggleComplete(
        @PathVariable id: String,
        @RequestBody form: ReplyForm,
        request: HttpServletRequest,
    ): ResponseEntity<Map<String, Any>> =
        runCatching {
            feedbackService.completeWithReply(id, form.content, urlBuilder.feedbackView(request, id))
        }.fold(
            onSuccess = { ResponseEntity.ok(statusBody(id)) },
            onFailure = { ResponseEntity.badRequest().body(errorBody(it)) },
        )

    /** 목록 행의 상태 태그를 바로 갱신할 수 있도록 최신 상태를 함께 돌려준다. */
    private fun statusBody(id: String): Map<String, Any> {
        val detail = feedbackService.getDetail(id)
        return mapOf(
            "ok" to true,
            "status" to detail.status.name,
            "processedAt" to (detail.processedAt?.toEpochMilli() ?: ""),
        )
    }

    private fun errorBody(error: Throwable): Map<String, Any> = mapOf("ok" to false, "message" to (error.message ?: "처리하지 못했습니다."))
}

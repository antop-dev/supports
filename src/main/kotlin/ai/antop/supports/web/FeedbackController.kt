package ai.antop.supports.web

import ai.antop.supports.common.FileKind
import ai.antop.supports.common.HtmlSanitizer
import ai.antop.supports.config.AppProperties
import ai.antop.supports.dto.FeedbackForm
import ai.antop.supports.exception.InvalidUploadException
import ai.antop.supports.service.FeedbackService
import ai.antop.supports.service.FileStorageService
import ai.antop.supports.service.SlackNotifier
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import org.springframework.context.MessageSource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import java.nio.charset.StandardCharsets
import java.util.Locale

@Controller
class FeedbackController(
    private val feedbackService: FeedbackService,
    private val projectService: ai.antop.supports.service.ProjectService,
    private val fileStorageService: FileStorageService,
    private val slackNotifier: SlackNotifier,
    private val appProperties: AppProperties,
    private val captchaValidator: CaptchaValidator,
    private val urlBuilder: UrlBuilder,
    private val messageSource: MessageSource,
) {
    @GetMapping("/")
    fun home(): String = "redirect:/feedbacks/new"

    /**
     * 의견 등록 폼. `?project=` 파라미터로 프로젝트를 미리 선택할 수 있다.
     * 값은 프로젝트의 공개 코드다 — PK 는 외부에 노출하지 않으므로 받지 않는다.
     * 없는 코드이거나 비활성 프로젝트면 선택하지 않은 상태로 둔다.
     */
    @GetMapping("/feedbacks/new")
    fun newForm(
        @RequestParam(name = "project", required = false) project: String?,
        model: Model,
    ): String {
        val projects = projectService.listEnabled()
        val form = model.getAttribute("form") as? FeedbackForm ?: FeedbackForm().also { model.addAttribute("form", it) }
        if (form.projectId.isBlank()) {
            projectService.findEnabledByCode(project)?.let { form.projectId = it.id }
        }
        model.addAttribute("projects", projects)
        // 첫 화면부터 선택된 프로젝트명이 보이도록 서버에서 내려준다(선택 팝업 스크립트를 기다리지 않는다).
        model.addAttribute("selectedProjectName", projects.firstOrNull { it.id == form.projectId }?.name)
        return "user/form"
    }

    /**
     * 의견 등록. 폼은 AJAX(multipart)로 전송되며, 검증은 서버에서 수행하고 결과를 JSON으로 반환한다.
     * 파일은 여기서 직접 받지 않는다 — 청크 업로드([FeedbackUploadController])로 미리 올려둔
     * 임시 파일의 id 를 "files" 파라미터로 받아 [FeedbackService.create] 에서 확정(claim)한다.
     *  - 검증 실패: 400 + { fieldErrors: {필드명: 메시지}, globalErrors: [메시지] }
     *  - 성공     : 200 + { redirect: "완료 페이지 URL" }
     */
    @PostMapping("/feedbacks")
    @ResponseBody
    fun create(
        @Valid @ModelAttribute("form") form: FeedbackForm,
        bindingResult: BindingResult,
        session: HttpSession,
        request: HttpServletRequest,
        locale: Locale,
    ): ResponseEntity<Map<String, Any>> {
        if (!captchaValidator.validate(session, form.captcha)) {
            bindingResult.rejectValue("captcha", "captcha.invalid")
        }
        if (form.content.isNotBlank() && HtmlSanitizer.isBlank(form.content)) {
            bindingResult.rejectValue("content", "feedback.content.required")
        }
        // @RequestParam("files") 는 파일이 선택되지 않아도 브라우저가 함께 보내는 빈 파일 파트를
        // MultipartFile 로 우선 바인딩해버려 List<String> 변환에 실패한다. 컨테이너가 파일이 아닌
        // 파트만 채워주는 request 파라미터에서 직접 읽어 그 문제를 피한다.
        val fileIdList = request.getParameterValues("files")?.filter { it.isNotBlank() } ?: emptyList()
        if (fileIdList.size > appProperties.upload.maxFiles) {
            bindingResult.reject("feedback.files.max", arrayOf(appProperties.upload.maxFiles), null)
        }

        if (bindingResult.hasErrors()) {
            val fieldErrors = bindingResult.fieldErrors.associate { it.field to messageSource.getMessage(it, locale) }
            val globalErrors = bindingResult.globalErrors.map { messageSource.getMessage(it, locale) }
            val body: Map<String, Any> = mapOf("fieldErrors" to fieldErrors, "globalErrors" to globalErrors)
            return ResponseEntity.badRequest().body(body)
        }

        val feedback =
            try {
                feedbackService.create(form, fileIdList, locale)
            } catch (_: InvalidUploadException) {
                val message = messageSource.getMessage("feedback.files.invalid", null, locale)
                val body: Map<String, Any> = mapOf("fieldErrors" to emptyMap<String, String>(), "globalErrors" to listOf(message))
                return ResponseEntity.badRequest().body(body)
            }
        slackNotifier.notifyFeedbackCreated(feedback.project.name, feedback.title, urlBuilder.adminFeedback(request, feedback.id))

        val body: Map<String, Any> = mapOf("redirect" to "${request.contextPath}/feedbacks/${feedback.id}/complete")
        return ResponseEntity.ok(body)
    }

    @GetMapping("/feedbacks/{id}/complete")
    fun complete(
        @PathVariable id: String,
        request: HttpServletRequest,
        model: Model,
    ): String {
        model.addAttribute("feedback", feedbackService.getDetail(id))
        model.addAttribute("viewUrl", urlBuilder.feedbackView(request, id))
        return "user/complete"
    }

    @GetMapping("/feedbacks/lookup")
    fun lookupForm(model: Model): String {
        model.addAttribute("recentFeedbacks", feedbackService.listRecentPublic())
        return "user/lookup"
    }

    /**
     * 접수번호 검색(AJAX). 검색 버튼의 인디케이터/쓰로틀링은 클라이언트(lookup.js)가 담당하고,
     * 여기서는 찾으면 이동할 주소만 내려준다.
     *  - 못 찾음: 404 + { fieldErrors: { receiptNo: 메시지 } }
     *  - 찾음  : 200 + { redirect: "상세 페이지 URL" }
     */
    @PostMapping("/feedbacks/lookup")
    @ResponseBody
    fun lookup(
        @RequestParam("receiptNo") receiptNo: String,
        request: HttpServletRequest,
        locale: Locale,
    ): ResponseEntity<Map<String, Any>> {
        val id = feedbackService.findIdByReceiptNo(receiptNo)
        if (id == null) {
            val message = messageSource.getMessage("lookup.notFound", null, locale)
            val body: Map<String, Any> = mapOf("fieldErrors" to mapOf("receiptNo" to message), "globalErrors" to emptyList<String>())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body)
        }
        val body: Map<String, Any> = mapOf("redirect" to "${request.contextPath}/feedbacks/$id")
        return ResponseEntity.ok(body)
    }

    @GetMapping("/feedbacks/{id}")
    fun view(
        @PathVariable id: String,
        session: HttpSession,
        model: Model,
    ): String {
        if (feedbackService.isSecret(id) && !isUnlocked(session, id)) {
            model.addAttribute("feedbackId", id)
            return "user/password"
        }
        model.addAttribute("feedback", feedbackService.getDetail(id))
        return "user/view"
    }

    @PostMapping("/feedbacks/{id}/unlock")
    fun unlock(
        @PathVariable id: String,
        @RequestParam("password") password: String,
        @RequestParam("captcha", required = false) captcha: String?,
        session: HttpSession,
        model: Model,
    ): String {
        if (!captchaValidator.validate(session, captcha)) {
            model.addAttribute("feedbackId", id)
            model.addAttribute("captchaError", true)
            return "user/password"
        }
        if (feedbackService.verifyPassword(id, password)) {
            markUnlocked(session, id)
            return "redirect:/feedbacks/$id"
        }
        model.addAttribute("feedbackId", id)
        model.addAttribute("passwordError", true)
        return "user/password"
    }

    @GetMapping("/feedbacks/{id}/files/{fileId}")
    fun download(
        @PathVariable id: String,
        @PathVariable fileId: String,
        session: HttpSession,
    ): ResponseEntity<Resource> {
        val info = feedbackService.getFileForDownload(id, fileId)
        if (info.secret && !isUnlocked(session, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        val path = fileStorageService.load(info.feedbackId, info.storedName)
        val resource = FileSystemResource(path)
        if (!resource.exists()) {
            return ResponseEntity.notFound().build()
        }
        // 이미지는 페이지 안 팝업으로, PDF는 새 탭의 브라우저 뷰어로 바로 보여줄 수 있도록 inline 으로 내린다.
        // 그 외 타입은 지금까지와 같이 다운로드(attachment) 된다.
        val kind = FileKind.from(info.contentType)
        val dispositionBuilder = if (kind == FileKind.OTHER) ContentDisposition.attachment() else ContentDisposition.inline()
        val disposition = dispositionBuilder.filename(info.originalName, StandardCharsets.UTF_8).build()
        return ResponseEntity
            .ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .contentType(downloadMediaType(info.contentType, kind))
            .body(resource)
    }

    /**
     * 첨부 파일의 Content-Type 은 업로드한 쪽이 보낸 값이라 그대로 믿지 않는다.
     * HTML/SVG 처럼 브라우저에서 스크립트가 실행될 수 있는 타입은 다운로드 전용으로 내린다.
     */
    private fun downloadMediaType(
        contentType: String?,
        kind: FileKind,
    ): MediaType {
        val parsed =
            contentType?.let { runCatching { MediaType.parseMediaType(it) }.getOrNull() }
                ?: return MediaType.APPLICATION_OCTET_STREAM
        val isSafe =
            kind == FileKind.IMAGE ||
                kind == FileKind.PDF ||
                SAFE_DOWNLOAD_TYPES.any { parsed.isCompatibleWith(MediaType.parseMediaType(it)) }
        return if (isSafe) parsed else MediaType.APPLICATION_OCTET_STREAM
    }

    @Suppress("UNCHECKED_CAST")
    private fun isUnlocked(
        session: HttpSession,
        id: String,
    ): Boolean {
        val set = session.getAttribute(SessionKeys.UNLOCKED_FEEDBACKS) as? MutableSet<String>
        return set?.contains(id) == true
    }

    @Suppress("UNCHECKED_CAST")
    private fun markUnlocked(
        session: HttpSession,
        id: String,
    ) {
        val set =
            (session.getAttribute(SessionKeys.UNLOCKED_FEEDBACKS) as? MutableSet<String>)
                ?: hashSetOf<String>().also { session.setAttribute(SessionKeys.UNLOCKED_FEEDBACKS, it) }
        set += id
    }

    companion object {
        /** 그대로 내려도 안전한 첨부 타입(이미지·PDF는 FileKind 로 별도 처리한다). */
        private val SAFE_DOWNLOAD_TYPES = listOf("text/plain")
    }
}

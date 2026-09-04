package ai.antop.supports.web

import ai.antop.supports.exception.InvalidUploadException
import ai.antop.supports.service.FileStorageService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

/**
 * 의견 등록 폼의 파일 첨부를 위한 청크 업로드 엔드포인트(FilePond 청크 업로드 프로토콜).
 *  - 대용량 파일도 작은 청크로 나눠 보내므로 느린 회선에서도 전체 요청이 한 번에 실패하지 않는다.
 *  - 여기서 만든 id 는 아직 어떤 의견에도 속하지 않은 임시 파일을 가리킨다.
 *    실제 첨부는 /feedbacks 등록이 성공할 때 [FileStorageService.claim] 으로 확정된다.
 */
@RestController
@RequestMapping("/feedbacks/uploads")
class FeedbackUploadController(
    private val fileStorageService: FileStorageService,
) {
    /** 업로드 시작. 청크가 아직 없으므로 전체 크기만 받아 임시 id 를 내려준다. */
    @PostMapping
    @ResponseBody
    fun begin(
        @RequestHeader("Upload-Length") uploadLength: String,
    ): ResponseEntity<String> {
        val length = uploadLength.toLongOrNull() ?: return ResponseEntity.badRequest().build()
        return try {
            val id = fileStorageService.beginUpload(length)
            ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(id)
        } catch (e: InvalidUploadException) {
            ResponseEntity.badRequest().body(e.message)
        }
    }

    /** 청크 하나를 지정한 오프셋에 기록한다. */
    @PatchMapping("/{id}")
    fun patchChunk(
        @PathVariable id: String,
        @RequestHeader("Upload-Offset") uploadOffset: String,
        @RequestHeader(value = "Upload-Name", required = false) uploadName: String?,
        request: HttpServletRequest,
    ): ResponseEntity<Void> {
        val offset = uploadOffset.toLongOrNull() ?: return ResponseEntity.badRequest().build()
        val chunkLength = request.contentLengthLong
        if (chunkLength < 0) {
            return ResponseEntity.badRequest().build()
        }
        // FilePond는 Upload-Name 헤더에 파일명을 퍼센트 인코딩 없이 그대로 담아 보낸다.
        // 그런데 서블릿 컨테이너(Tomcat)는 HTTP 헤더 바이트를 기본적으로 ISO-8859-1로 해석하므로,
        // 한글처럼 UTF-8로 여러 바이트인 문자는 여기 도착했을 때 이미 깨져 있다(각 바이트가 라틴 문자 하나로 잘못 해석됨).
        // 원래 UTF-8 바이트로 되돌리기 위해 ISO-8859-1로 재인코딩한 뒤 UTF-8로 다시 디코딩한다.
        val originalName =
            uploadName?.let {
                runCatching { String(it.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8) }.getOrNull()
            }
        return try {
            fileStorageService.writeChunk(id, offset, chunkLength, originalName, request.inputStream)
            ResponseEntity.ok().build()
        } catch (_: InvalidUploadException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).build()
        }
    }

    /** 업로드 취소(사용자가 첨부를 제거하거나, 등록 실패 후 다시 시도할 때). id 는 본문에 평문으로 온다. */
    @DeleteMapping
    fun revert(request: HttpServletRequest): ResponseEntity<Void> {
        val id = request.reader.readText().trim()
        if (id.isNotEmpty()) {
            fileStorageService.discardUpload(id)
        }
        return ResponseEntity.ok().build()
    }
}

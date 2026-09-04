package ai.antop.supports.common

import org.springframework.http.MediaType

/**
 * 첨부 파일의 표시 방식을 Content-Type 기준으로 나눈다.
 *  - IMAGE: 페이지 안에서 팝업으로 바로 보여준다.
 *  - PDF: 새 창(새 탭)에서 브라우저 뷰어로 바로 연다.
 *  - OTHER: 다운로드한다.
 */
enum class FileKind {
    IMAGE,
    PDF,
    OTHER,
    ;

    companion object {
        fun from(contentType: String?): FileKind {
            val parsed = contentType?.let { runCatching { MediaType.parseMediaType(it) }.getOrNull() } ?: return OTHER
            return when {
                // SVG는 브라우저에서 스크립트가 실행될 수 있어 이미지로 취급하지 않는다.
                parsed.type.equals("image", ignoreCase = true) && !parsed.subtype.equals("svg+xml", ignoreCase = true) -> IMAGE
                parsed.isCompatibleWith(MediaType.APPLICATION_PDF) -> PDF
                else -> OTHER
            }
        }
    }
}

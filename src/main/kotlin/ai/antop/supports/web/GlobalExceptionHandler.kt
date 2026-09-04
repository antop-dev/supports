package ai.antop.supports.web

import ai.antop.supports.exception.NotFoundException
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus

@ControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(NotFoundException::class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    fun handleNotFound(ex: NotFoundException): String {
        log.debug("404: {}", ex.message)
        return "error/404"
    }

    /**
     * 본문을 읽지 못한 요청(깨진 JSON 등).
     * 이 예외는 [ErrorResponse] 를 구현하지 않아 상태 코드를 들고 오지 않으므로 여기서 정한다.
     * 쓰기 실패([org.springframework.http.converter.HttpMessageNotWritableException])는
     * 서버 잘못이므로 여기에 포함하지 않는다(아래에서 500 으로 처리된다).
     */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleUnreadableBody(ex: HttpMessageNotReadableException): String {
        log.debug("400: {}", ex.message)
        return "error/400"
    }

    /**
     * 나머지 전부.
     *
     * 스프링이 던지는 요청 오류(매핑 없는 주소, 허용되지 않는 메서드, 협상 불가한 Accept,
     * 읽을 수 없는 본문 등)는 [ErrorResponse] 를 구현해 상태 코드를 들고 온다.
     * 이걸 구분하지 않고 전부 500 으로 만들면 클라이언트 잘못이 서버 오류로 둔갑하고
     * 에러 로그만 쌓인다. 그래서 상태 코드를 예외에서 꺼내 그대로 돌려준다.
     *
     * 메서드에 `@ResponseStatus` 를 붙이지 않는 이유는 예외마다 상태가 다르기 때문이다.
     */
    @ExceptionHandler(Exception::class)
    fun handleGeneral(
        ex: Exception,
        response: HttpServletResponse,
    ): String {
        val status: HttpStatusCode = (ex as? ErrorResponse)?.statusCode ?: HttpStatus.INTERNAL_SERVER_ERROR
        response.status = status.value()

        if (status.is5xxServerError) {
            log.error("Unhandled exception", ex)
            return "error/500"
        }
        // 클라이언트가 잘못 보낸 요청이므로 서버 오류로 기록하지 않는다.
        log.debug("{} {}: {}", status.value(), ex.javaClass.simpleName, ex.message)
        return if (status.value() == HttpStatus.NOT_FOUND.value()) "error/404" else "error/400"
    }
}

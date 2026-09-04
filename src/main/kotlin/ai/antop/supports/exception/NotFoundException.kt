package ai.antop.supports.exception

/** 요청한 리소스를 찾을 수 없을 때. */
class NotFoundException(
    message: String = "not found",
) : RuntimeException(message)

package ai.antop.supports.exception

/** 청크 업로드 id 를 찾을 수 없거나, 아직 완료되지 않았거나, 크기/청크 조건을 위반했을 때. */
class InvalidUploadException(
    message: String,
) : RuntimeException(message)

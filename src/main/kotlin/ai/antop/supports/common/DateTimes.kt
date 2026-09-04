package ai.antop.supports.common

import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 템플릿에서 Instant 를 표시용 문자열로 바꾼다. (`${@dateTimes.format(...)}`)
 */
@Component("dateTimes")
class DateTimes {
    /**
     * 사용자 화면용. 최종 표시 값은 localtime.js 가 브라우저 시간대로 다시 그리지만,
     * 스크립트 실행 전에도 완성된 화면이 보이도록 서버 시간대 기준 값을 미리 렌더링한다.
     */
    fun format(value: Instant?): String = value?.let { FORMATTER.format(it) } ?: "-"

    /** 관리자 화면용. 보는 사람의 시간대와 상관없이 항상 KST 로 보여준다. */
    fun formatKst(value: Instant?): String = value?.let { KST_FORMATTER.format(it) } ?: "-"

    companion object {
        /** 관리자 화면 기준 시간대. 표시와 날짜 검색이 같은 기준을 쓰도록 여기서만 정의한다. */
        val KST: ZoneId = ZoneId.of("Asia/Seoul")

        private val PATTERN: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

        private val FORMATTER: DateTimeFormatter = PATTERN.withZone(ZoneId.systemDefault())

        private val KST_FORMATTER: DateTimeFormatter = PATTERN.withZone(KST)
    }
}

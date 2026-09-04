package ai.antop.supports.service

import java.util.Locale

/** 처리 완료 메일 한 통을 만드는 데 필요한 값. */
data class DoneMail(
    val toEmail: String,
    val receiptNo: String,
    val projectName: String,
    val title: String,
    /** 관리자가 남긴 처리 내용(새니타이징된 HTML). */
    val replyContent: String?,
    /** 접수자가 자기 의견을 확인하는 화면 주소. */
    val viewUrl: String,
    /** 접수 당시 사용자의 언어. */
    val locale: Locale,
)

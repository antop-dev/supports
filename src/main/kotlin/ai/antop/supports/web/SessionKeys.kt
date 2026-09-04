package ai.antop.supports.web

object SessionKeys {
    const val CAPTCHA = "CAPTCHA_CODE"
    const val UNLOCKED_FEEDBACKS = "UNLOCKED_FEEDBACKS"
    const val ADMIN = "ADMIN"

    /** 로그인 전에 들어오려던 관리자 화면 주소. 로그인에 성공하면 이 주소로 보낸다. */
    const val ADMIN_REDIRECT = "ADMIN_REDIRECT"
}

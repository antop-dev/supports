package ai.antop.supports.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/** 사용자 의견 등록 폼. */
data class FeedbackForm(
    @field:NotBlank(message = "{feedback.project.required}")
    var projectId: String = "",
    @field:NotBlank(message = "{feedback.title.required}")
    @field:Size(max = 200, message = "{feedback.title.size}")
    var title: String = "",
    @field:Email(message = "{feedback.email.invalid}")
    var email: String? = null,
    /** 처리 결과를 이메일로 받을지. 이메일을 입력했을 때만 의미가 있다. */
    var emailReply: Boolean = false,
    @field:NotBlank(message = "{feedback.content.required}")
    var content: String = "",
    var password: String? = null,
    @field:NotBlank(message = "{captcha.required}")
    var captcha: String = "",
)

/** 관리자 처리 내용 저장 폼. */
data class ReplyForm(
    var content: String = "",
)

/** 관리자 프로젝트 등록/수정 폼. 관리자 화면은 다국어를 쓰지 않으므로 문구를 그대로 둔다. */
data class ProjectForm(
    @field:NotBlank(message = "프로젝트명을 입력하세요.")
    @field:Size(max = 100, message = "프로젝트명은 최대 100자까지 입력할 수 있습니다.")
    var name: String = "",
    /** 접수 폼 링크에 쓰는 공개 코드. 비우면 프로젝트명에서 자동 생성한다. */
    var code: String? = null,
    var enabled: Boolean = true,
    var sortOrder: Int = 0,
    var url: String? = null,
)

/** 관리자 프로젝트 셀 단위 수정 요청. */
data class CellUpdateRequest(
    val field: String = "",
    val value: String = "",
)

/** 관리자 계정 등록 폼(AJAX). */
data class AdminUserForm(
    val username: String = "",
    val password: String = "",
)

/** 관리자 본인 비밀번호 변경 폼. 관리자 화면은 다국어를 쓰지 않으므로 문구를 그대로 둔다. */
data class PasswordChangeForm(
    @field:NotBlank(message = "현재 비밀번호를 입력하세요.")
    var currentPassword: String = "",
    @field:NotBlank(message = "새 비밀번호를 입력하세요.")
    @field:Size(min = 8, message = "새 비밀번호는 8자 이상이어야 합니다.")
    var newPassword: String = "",
    @field:NotBlank(message = "새 비밀번호 확인을 입력하세요.")
    var confirmPassword: String = "",
)

/** 관리자 로그인 폼. 관리자 화면은 다국어를 쓰지 않으므로 문구를 그대로 둔다. */
data class LoginForm(
    @field:NotBlank(message = "아이디를 입력하세요.")
    var username: String = "",
    @field:NotBlank(message = "비밀번호를 입력하세요.")
    var password: String = "",
    @field:NotBlank(message = "자동입력 방지 문자를 입력하세요.")
    var captcha: String = "",
)

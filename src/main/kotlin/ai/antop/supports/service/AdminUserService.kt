package ai.antop.supports.service

import ai.antop.supports.domain.AdminUser
import ai.antop.supports.dto.AdminUserItem
import ai.antop.supports.exception.NotFoundException
import ai.antop.supports.repository.AdminUserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminUserService(
    private val adminUserRepository: AdminUserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 아이디/비밀번호가 일치하는 관리자 계정이 있으면 true.
     * 계정은 admin_users 테이블에만 있고, 설정 파일로는 만들지 않는다.
     */
    fun authenticate(
        username: String,
        rawPassword: String,
    ): Boolean =
        adminUserRepository
            .findByUsername(username.trim())
            ?.let { user ->
                // 해시는 {bcrypt} 같은 접두사가 있어야 한다. 손으로 넣은 값이 형식에 안 맞으면
                // 예외 대신 로그인 실패로 처리하고 원인을 로그로 남긴다.
                runCatching { passwordEncoder.matches(rawPassword, user.passwordHash) }
                    .onFailure { log.warn("Invalid admin password hash format. username={}", user.username, it) }
                    .getOrDefault(false)
            } ?: false

    fun pageList(
        username: String?,
        pageable: Pageable,
    ): Page<AdminUserItem> =
        adminUserRepository
            .search(username = username?.trim()?.takeIf { it.isNotBlank() }, pageable = pageable)
            .map(AdminUserItem::from)

    @Transactional
    fun create(
        username: String,
        rawPassword: String,
    ): AdminUser {
        val name = username.trim()
        require(name.length in MIN_USERNAME_LENGTH..MAX_USERNAME_LENGTH) {
            "아이디는 ${MIN_USERNAME_LENGTH}~${MAX_USERNAME_LENGTH}자로 입력하세요."
        }
        require(USERNAME_PATTERN.matches(name)) { "아이디는 영문/숫자/._- 만 사용할 수 있습니다." }
        require(!adminUserRepository.existsByUsername(name)) { "이미 사용 중인 아이디입니다." }
        require(rawPassword.length >= MIN_PASSWORD_LENGTH) { "비밀번호는 ${MIN_PASSWORD_LENGTH}자 이상이어야 합니다." }
        return adminUserRepository.save(AdminUser(username = name, passwordHash = checkNotNull(passwordEncoder.encode(rawPassword))))
    }

    @Transactional
    fun delete(
        id: String,
        currentUsername: String,
    ) {
        val user = adminUserRepository.findByIdOrNull(id) ?: throw NotFoundException("admin user not found: $id")
        require(user.username != currentUsername) { "현재 로그인한 계정은 삭제할 수 없습니다." }
        require(adminUserRepository.count() > 1L) { "관리자 계정은 최소 1개 이상 있어야 합니다." }
        adminUserRepository.delete(user)
    }

    /** 본인 비밀번호 변경. 현재 비밀번호가 일치해야 한다. */
    @Transactional
    fun changePassword(
        username: String,
        currentPassword: String,
        newPassword: String,
    ) {
        val user =
            adminUserRepository.findByUsername(username)
                ?: throw NotFoundException("admin user not found: $username")
        require(passwordEncoder.matches(currentPassword, user.passwordHash)) { "현재 비밀번호가 일치하지 않습니다." }
        require(newPassword.length >= MIN_PASSWORD_LENGTH) { "새 비밀번호는 ${MIN_PASSWORD_LENGTH}자 이상이어야 합니다." }
        require(!passwordEncoder.matches(newPassword, user.passwordHash)) { "현재 비밀번호와 다른 비밀번호를 입력하세요." }
        user.passwordHash = checkNotNull(passwordEncoder.encode(newPassword))
    }

    companion object {
        private const val MIN_USERNAME_LENGTH = 3
        private const val MAX_USERNAME_LENGTH = 50
        private const val MIN_PASSWORD_LENGTH = 8
        private val USERNAME_PATTERN = Regex("^[A-Za-z0-9._-]+$")
    }
}

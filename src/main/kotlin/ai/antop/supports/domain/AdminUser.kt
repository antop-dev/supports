package ai.antop.supports.domain

import ai.antop.supports.common.IdGenerator
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/** 관리자 계정. 비밀번호는 BCrypt 해시로만 보관한다. */
@Entity
@Table(name = "admin_users")
class AdminUser(
    @Column(name = "username", nullable = false, unique = true)
    var username: String,
    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: String = IdGenerator.newId(),
) : BaseTimeEntity()

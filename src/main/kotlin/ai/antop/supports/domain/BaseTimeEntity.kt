package ai.antop.supports.domain

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import java.time.Instant

/** 생성/수정 시각을 UTC(Instant)로 관리하는 공통 상위 타입. */
@MappedSuperclass
abstract class BaseTimeEntity {
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()
        protected set

    @Column(name = "updated_at")
    var updatedAt: Instant? = null
        protected set

    @PrePersist
    fun onCreate() {
        val now = Instant.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = Instant.now()
    }
}

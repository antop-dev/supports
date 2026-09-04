package ai.antop.supports.domain

import ai.antop.supports.common.IdGenerator
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/** 의견을 접수할 대상 프로젝트. 관리자가 관리한다. */
@Entity
@Table(name = "projects")
class Project(
    @Column(name = "name", nullable = false)
    var name: String,
    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true,
    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0,
    @Column(name = "url")
    var url: String? = null,
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    val id: String = IdGenerator.newId(),
) : BaseTimeEntity()

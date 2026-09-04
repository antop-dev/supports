package ai.antop.supports.common

import com.github.f4b6a3.ulid.UlidCreator

/** ULID 기반 PK 생성기. */
object IdGenerator {
    fun newId(): String = UlidCreator.getMonotonicUlid().toString()
}

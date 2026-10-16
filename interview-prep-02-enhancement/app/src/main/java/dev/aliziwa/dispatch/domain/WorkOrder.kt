package dev.aliziwa.dispatch.domain

enum class Priority { HIGH, MEDIUM, LOW }

enum class Status {
    OPEN, IN_PROGRESS, DONE;

    fun next(): Status? = when (this) {
        OPEN -> IN_PROGRESS
        IN_PROGRESS -> DONE
        DONE -> null
    }
}

/**
 * A job assigned to a technician. [version] is the server's optimistic-concurrency token:
 * every server-side change increments it, and a write that carries a stale version is rejected.
 */
data class WorkOrder(
    val id: String,
    val title: String,
    val site: String,
    val priority: Priority,
    val status: Status,
    val version: Int,
    val updatedAt: Long,
    val isPending: Boolean = false,
)

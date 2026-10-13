package dev.aliziwa.fieldnotes.sync

/**
 * Process-wide sync event bus. Screens register to be told when a sync lands.
 */
object SyncManager {

    fun interface Listener {
        fun onSyncFinished(added: Int)
    }

    private val listeners = mutableListOf<Listener>()

    var completedSyncs: Int = 0
        private set

    fun addListener(listener: Listener) {
        listeners += listener
    }

    fun removeListener(listener: Listener) {
        listeners -= listener
    }

    fun notifyFinished(added: Int) {
        completedSyncs += 1
        listeners.forEach { it.onSyncFinished(added) }
    }
}

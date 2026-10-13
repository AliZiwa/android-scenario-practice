package dev.aliziwa.fieldnotes.data.remote

import kotlinx.coroutines.delay

data class RemoteNote(
    val id: String,
    val title: String,
    val body: String,
    val updatedAt: Long,
)

interface RemoteNotesApi {
    suspend fun fetchNotes(): List<RemoteNote>
}

/**
 * Stands in for a REST backend. Deterministic data, realistic latency, no network needed.
 * Each call returns the same five notes, the way an idempotent GET /notes would.
 */
class FakeRemoteNotesApi(
    private val latencyMs: Long = 800,
) : RemoteNotesApi {

    override suspend fun fetchNotes(): List<RemoteNote> {
        delay(latencyMs)
        return SEED
    }

    private companion object {
        val SEED = listOf(
            RemoteNote("srv-1001", "Lift inspection, Bay 3", "Hydraulic seal weeping. Ordered part #HS-204.", 1_760_000_000_000),
            RemoteNote("srv-1002", "Tire rotation checklist", "Torque to spec, reset TPMS, note tread depth per wheel.", 1_760_000_100_000),
            RemoteNote("srv-1003", "Customer callback: Okello", "Wants quote for brake pads and rotors, front only.", 1_760_000_200_000),
            RemoteNote("srv-1004", "Oil bay drain pan", "Replace cracked pan before Monday shift.", 1_760_000_300_000),
            RemoteNote("srv-1005", "Locker 7 sensor", "Intermittent 'door open' reading. Check wiring harness.", 1_760_000_400_000),
        )
    }
}

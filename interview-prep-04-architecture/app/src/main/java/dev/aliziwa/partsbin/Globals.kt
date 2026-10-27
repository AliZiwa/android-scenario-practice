package dev.aliziwa.partsbin

import android.annotation.SuppressLint
import android.app.Application
import dev.aliziwa.partsbin.db.PartsDatabase

/**
 * Process-wide state. Whatever needs to be reached from anywhere lives here.
 */
@SuppressLint("StaticFieldLeak")
object Globals {
    lateinit var app: Application
    lateinit var db: PartsDatabase

    /** Which part the detail screen should show. Set by the list before navigating. */
    var selectedPartId: String? = null

    /** Flipped by the switch in the top bar; read by the fake backend. */
    @Volatile
    var offline: Boolean = false

    /** Bumped after an adjustment so the list knows to reload when it comes back. */
    var listDirty: Boolean = false
}

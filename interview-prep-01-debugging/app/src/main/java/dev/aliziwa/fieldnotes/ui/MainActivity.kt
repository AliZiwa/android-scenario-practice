package dev.aliziwa.fieldnotes.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.VisibleForTesting
import androidx.compose.material3.MaterialTheme
import dev.aliziwa.fieldnotes.FieldNotesApp
import dev.aliziwa.fieldnotes.di.AppContainer
import dev.aliziwa.fieldnotes.sync.SyncManager

class MainActivity : ComponentActivity() {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var container: AppContainer? = null
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    var toastListener: SyncManager.Listener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = (application as FieldNotesApp).container
        toastListener = SyncManager.Listener { added -> Toast.makeText(this@MainActivity, "Sync finished: $added new", Toast.LENGTH_SHORT).show() }
        toastListener?.let {
            SyncManager.addListener(it)
        }

        setContent {
            MaterialTheme {
                container?.let {
                    FieldNotesNavHost(it)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        container = null
        toastListener?.let {
            SyncManager.removeListener(it)
        }
    }
}

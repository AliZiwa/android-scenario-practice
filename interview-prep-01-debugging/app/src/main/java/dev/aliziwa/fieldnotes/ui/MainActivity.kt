package dev.aliziwa.fieldnotes.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import dev.aliziwa.fieldnotes.FieldNotesApp
import dev.aliziwa.fieldnotes.sync.SyncManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as FieldNotesApp).container

        SyncManager.addListener { added ->
            Toast.makeText(this@MainActivity, "Sync finished: $added new", Toast.LENGTH_SHORT).show()
        }

        setContent {
            MaterialTheme {
                FieldNotesNavHost(container)
            }
        }
    }
}

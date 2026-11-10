package dev.aliziwa.partsbin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import dev.aliziwa.partsbin.ui.PartsBinNavHost

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val container = (application as PartsBinApp).container
            MaterialTheme {
                PartsBinNavHost(container)
            }
        }
    }
}

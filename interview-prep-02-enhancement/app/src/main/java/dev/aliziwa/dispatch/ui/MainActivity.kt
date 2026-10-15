package dev.aliziwa.dispatch.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import dev.aliziwa.dispatch.DispatchApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as DispatchApp).container
        setContent {
            MaterialTheme {
                DispatchNavHost(container)
            }
        }
    }
}

package dev.aliziwa.partsbin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.aliziwa.partsbin.ui.PartsBinNavHost
import dev.aliziwa.partsbin.ui.screens.PartDetailScreen
import dev.aliziwa.partsbin.ui.screens.PartsListScreen

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

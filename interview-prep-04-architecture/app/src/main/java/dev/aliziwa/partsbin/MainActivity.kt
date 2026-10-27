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
import androidx.room.Room
import dev.aliziwa.partsbin.db.PartsDatabase
import dev.aliziwa.partsbin.screens.PartDetailScreen
import dev.aliziwa.partsbin.screens.PartsListScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Globals.db = Room.databaseBuilder(this, PartsDatabase::class.java, "parts.db")
            .allowMainThreadQueries()
            .build()
        setContent {
            MaterialTheme {
                var screen by remember { mutableStateOf("list") }
                when (screen) {
                    "list" -> PartsListScreen(onOpenPart = { screen = "detail" })
                    "detail" -> {
                        BackHandler { screen = "list" }
                        PartDetailScreen(onBack = { screen = "list" })
                    }
                }
            }
        }
    }
}

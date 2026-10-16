package dev.aliziwa.timesheet.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import dev.aliziwa.timesheet.Injector

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Injector.init(applicationContext)
        setContent {
            MaterialTheme {
                TimesheetScreen()
            }
        }
    }
}

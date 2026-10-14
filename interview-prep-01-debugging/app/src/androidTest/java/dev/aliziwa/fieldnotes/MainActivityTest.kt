package dev.aliziwa.fieldnotes

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.aliziwa.fieldnotes.sync.SyncManager
import dev.aliziwa.fieldnotes.ui.MainActivity
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun onCreateInitializesAllProperties() {
        val scenario = composeTestRule.activityRule.scenario
        scenario.moveToState(Lifecycle.State.RESUMED)

        scenario.onActivity {
            assertTrue(it.container != null)
            assertTrue(it.toastListener != null)
        }
    }

    @Test
    fun onDestroyClearsAllProperties() {
        val scenario = composeTestRule.activityRule.scenario
        val activity = composeTestRule.activity

        scenario.moveToState(Lifecycle.State.DESTROYED)

        assertTrue(activity.container == null)
    }
}
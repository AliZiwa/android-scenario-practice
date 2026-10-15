package dev.aliziwa.fieldnotes

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.aliziwa.fieldnotes.ui.MainActivity
import junit.framework.TestCase.assertNotNull
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val detectLeakAfterTest = DetectLeaksAfterTestSuccess()

    @Test
    fun onCreateInitializesAllPropertiesAndRecreatesWithoutLeaks() {
        val scenario = composeTestRule.activityRule.scenario
        scenario.moveToState(Lifecycle.State.RESUMED)

        scenario.onActivity {
            assertNotNull(it.toastListener)
        }

        scenario.recreate()

        scenario.onActivity {
            assertNotNull(it.toastListener)
        }
    }
}

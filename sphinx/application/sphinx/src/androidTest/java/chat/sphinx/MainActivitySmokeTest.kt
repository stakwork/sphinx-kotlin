package chat.sphinx

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import chat.sphinx.activitymain.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MainActivitySmokeTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun launchMainActivityShowsPrimaryNavigationHost() {
        ActivityScenario.launch(MainActivity::class.java).use {
            onView(withId(R.id.layout_motion_main)).check(matches(isDisplayed()))
            onView(withId(R.id.nav_host_fragment_primary)).check(matches(isDisplayed()))
        }
    }
}

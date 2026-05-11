package chat.sphinx.dashboard

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DashboardLayoutRobolectricTest {

    @Test
    fun `dashboard layout exposes primary navigation affordances`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.setTheme(R.style.AppTheme)

        val dashboard = LayoutInflater.from(context)
            .inflate(R.layout.fragment_dashboard, null, false)

        assertNotNull(dashboard.findViewById(R.id.layout_dashboard_header))
        assertNotNull(dashboard.findViewById(R.id.tab_layout_dashboard_tabs))
        assertNotNull(dashboard.findViewById(R.id.view_pager_dashboard_tabs))
        assertNotNull(dashboard.findViewById(R.id.layout_dashboard_nav_bar))

        assertNotNull(dashboard.findViewById(R.id.nav_bar_button_payment_receive))
        assertNotNull(dashboard.findViewById(R.id.nav_bar_button_transactions))
        assertNotNull(dashboard.findViewById(R.id.nav_bar_button_scanner))
        assertNotNull(dashboard.findViewById(R.id.nav_bar_button_payment_send))

        val player = dashboard.findViewById<View>(R.id.swipe_reveal_layout_player)
        assertEquals(View.GONE, player.visibility)
    }
}

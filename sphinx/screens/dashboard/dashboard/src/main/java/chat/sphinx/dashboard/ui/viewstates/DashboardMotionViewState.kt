package chat.sphinx.dashboard.ui.viewstates

import androidx.constraintlayout.motion.widget.MotionLayout
import chat.sphinx.dashboard.R
import io.matthewnelson.android_concept_views.MotionLayoutViewState

internal sealed class DashboardMotionViewState: MotionLayoutViewState<DashboardMotionViewState>() {

    object DrawerCloseNavBarVisible: DashboardMotionViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_dashboard_drawer_open_nav_bar_visible
        override val endSetId: Int
            get() = R.id.motion_scene_dashboard_default

        override fun restoreMotionScene(motionLayout: MotionLayout) {}
    }

    object DrawerCloseNavBarHidden: DashboardMotionViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_dashboard_drawer_open_nav_bar_hidden
        override val endSetId: Int
            get() = R.id.motion_scene_dashboard_nav_bar_hidden

        override fun restoreMotionScene(motionLayout: MotionLayout) {
            motionLayout.setTransition(R.id.transition_dashboard_nav_drawer_open_to_close_nav_bar_hidden)
            motionLayout.setProgress(1F, 1F)
        }
    }

    object DrawerOpenNavBarHidden: DashboardMotionViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_dashboard_default
        override val endSetId: Int
            get() = R.id.motion_scene_dashboard_drawer_open_nav_bar_hidden

        override fun restoreMotionScene(motionLayout: MotionLayout) {
            motionLayout.setTransition(R.id.transition_dashboard_drawer_closed_to_open_nav_bar_hidden)
            motionLayout.setProgress(1F, 1F)
        }
    }

    object DrawerOpenNavBarVisible: DashboardMotionViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_dashboard_default
        override val endSetId: Int
            get() = R.id.motion_scene_dashboard_drawer_open_nav_bar_visible

        override fun restoreMotionScene(motionLayout: MotionLayout) {
            motionLayout.setTransition(R.id.transition_dashboard_drawer_closed_to_open_nav_bar_visible)
            motionLayout.setProgress(1F, 1F)
        }
    }
}

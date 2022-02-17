package chat.sphinx.dashboard.ui.viewstates

import androidx.constraintlayout.motion.widget.MotionLayout
import chat.sphinx.dashboard.R
import io.matthewnelson.android_concept_views.MotionLayoutViewState

internal sealed class DashboardMotionViewState: MotionLayoutViewState<DashboardMotionViewState>() {

    object Default: DashboardMotionViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_dashboard_drawer_open
        override val endSetId: Int
            get() = R.id.motion_scene_dashboard_default

        override fun restoreMotionScene(motionLayout: MotionLayout) {}
    }

    object DrawerOpen: DashboardMotionViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_dashboard_default
        override val endSetId: Int
            get() = R.id.motion_scene_dashboard_drawer_open

        override fun restoreMotionScene(motionLayout: MotionLayout) {
            motionLayout.setTransition(R.id.transition_dashboard_drawer_closed_to_open)
            motionLayout.setProgress(1F, 1F)
        }
    }

    object NavBarHidden: DashboardMotionViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_dashboard_default
        override val endSetId: Int
            get() = R.id.motion_scene_dashboard_nav_bar_hidden

        override fun restoreMotionScene(motionLayout: MotionLayout) {
            motionLayout.setTransition(R.id.transition_dashboard_nav_visible_to_hidden)
            motionLayout.setProgress(1F, 1F)
        }
    }
}

package chat.sphinx.dashboard.ui.viewstates

import androidx.constraintlayout.motion.widget.MotionLayout
import chat.sphinx.dashboard.R
import io.matthewnelson.android_concept_views.MotionLayoutViewState

internal sealed class NavDrawerViewState: MotionLayoutViewState<NavDrawerViewState>() {

    object Closed: NavDrawerViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_dashboard_drawer_open
        override val endSetId: Int
            get() = R.id.motion_scene_dashboard_drawer_closed

        override fun restoreMotionScene(motionLayout: MotionLayout) {}
    }

    object Open: NavDrawerViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_dashboard_drawer_closed
        override val endSetId: Int
            get() = R.id.motion_scene_dashboard_drawer_open

        override fun restoreMotionScene(motionLayout: MotionLayout) {
            motionLayout.setTransition(R.id.transition_dashboard_drawer_closed_to_open)
            motionLayout.setProgress(1F, 1F)
        }
    }
}
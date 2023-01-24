package chat.sphinx.tribes_discover.viewstate

import androidx.constraintlayout.motion.widget.MotionLayout
import chat.sphinx.tribes_discover.R
import io.matthewnelson.android_concept_views.MotionLayoutViewState

sealed class DiscoverTribesLoadingViewState:  MotionLayoutViewState<DiscoverTribesLoadingViewState>() {

    object Closed: DiscoverTribesLoadingViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_discover_tribes_loading_open
        override val endSetId: Int?
            get() = R.id.motion_scene_discover_tribes_loading_closed

        override fun restoreMotionScene(motionLayout: MotionLayout) {}
    }

    object Open: DiscoverTribesLoadingViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_discover_tribes_loading_closed
        override val endSetId: Int?
            get() = R.id.motion_scene_discover_tribes_loading_open

        override fun restoreMotionScene(motionLayout: MotionLayout) {
            motionLayout.setTransition(R.id.transition_discover_tribes_loading_closed_to_open)
            motionLayout.setProgress(1F, 1F)
        }
    }
}
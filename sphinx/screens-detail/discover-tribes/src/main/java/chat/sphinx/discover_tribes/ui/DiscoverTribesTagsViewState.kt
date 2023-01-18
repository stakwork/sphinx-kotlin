package chat.sphinx.discover_tribes.ui

import androidx.constraintlayout.motion.widget.MotionLayout
import chat.sphinx.discover_tribes.R
import io.matthewnelson.android_concept_views.MotionLayoutViewState

sealed class DiscoverTribesTagsViewState: MotionLayoutViewState<DiscoverTribesTagsViewState>() {

    object Closed: DiscoverTribesTagsViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_discover_tribes_open
        override val endSetId: Int?
            get() = R.id.motion_scene_discover_tribes_closed

        override fun restoreMotionScene(motionLayout: MotionLayout) {}
    }

    object Open: DiscoverTribesTagsViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_discover_tribes_closed
        override val endSetId: Int?
            get() = R.id.motion_scene_discover_tribes_open

        override fun restoreMotionScene(motionLayout: MotionLayout) {
            motionLayout.setTransition(R.id.transition_discover_tribes_tags_closed_to_open)
            motionLayout.setProgress(1F, 1F)
        }
    }
}
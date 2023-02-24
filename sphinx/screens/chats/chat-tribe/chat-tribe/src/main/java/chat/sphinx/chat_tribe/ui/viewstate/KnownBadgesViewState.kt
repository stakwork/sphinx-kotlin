package chat.sphinx.chat_tribe.ui.viewstate

import androidx.constraintlayout.motion.widget.MotionLayout
import chat.sphinx.chat_tribe.R
import io.matthewnelson.android_concept_views.MotionLayoutViewState

sealed class KnownBadgesViewState: MotionLayoutViewState<KnownBadgesViewState>() {

    object Closed: KnownBadgesViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_known_badges_open
        override val endSetId: Int?
            get() = R.id.motion_scene_known_badges_closed

        override fun restoreMotionScene(motionLayout: MotionLayout) {}
    }

    object Open: KnownBadgesViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_known_badges_closed
        override val endSetId: Int?
            get() = R.id.motion_scene_known_badges_open

        override fun restoreMotionScene(motionLayout: MotionLayout) {
            motionLayout.setTransition(R.id.transition_known_badges_closed_to_open)
            motionLayout.setProgress(1F, 1F)
        }
    }
}

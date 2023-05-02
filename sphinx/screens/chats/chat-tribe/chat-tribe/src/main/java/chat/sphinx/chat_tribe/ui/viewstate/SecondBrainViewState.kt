package chat.sphinx.chat_tribe.ui.viewstate

import androidx.constraintlayout.motion.widget.MotionLayout
import chat.sphinx.chat_tribe.R
import io.matthewnelson.android_concept_views.MotionLayoutViewState


sealed class SecondBrainViewState: MotionLayoutViewState<SecondBrainViewState>() {

    object Closed: SecondBrainViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_second_brain_open
        override val endSetId: Int?
            get() = R.id.motion_scene_second_brain_closed

        override fun restoreMotionScene(motionLayout: MotionLayout) {}
    }

    object Open: SecondBrainViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_second_brain_closed
        override val endSetId: Int?
            get() = R.id.motion_scene_second_brain_open

        override fun restoreMotionScene(motionLayout: MotionLayout) {
            motionLayout.setTransition(R.id.transition_second_brain_closed_to_open)
            motionLayout.setProgress(1F, 1F)
        }
    }

}
package chat.sphinx.activitymain.ui

import androidx.constraintlayout.motion.widget.MotionLayout
import chat.sphinx.activitymain.R
import io.matthewnelson.android_concept_views.MotionLayoutViewState

@Suppress("ClassName")
internal sealed class MainViewState: MotionLayoutViewState<MainViewState>() {

    object DetailScreenInactive: MainViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_main_set1
        override val endSetId: Int?
            get() = null

        override fun transitionToEndSet(motionLayout: MotionLayout) {}
        override fun restoreMotionScene(motionLayout: MotionLayout) {}
    }

    object Transition_DetailScreenActive: MainViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_main_set1
        override val endSetId: Int
            get() = R.id.motion_scene_main_set2
    }

    object DetailScreenActive: MainViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_main_set2
        override val endSetId: Int?
            get() = null

        override fun restoreMotionScene(motionLayout: MotionLayout) {
            motionLayout.setTransition(R.id.transition_main_set1_to_set2)
            motionLayout.setProgress(1F, 1F)
        }
    }

    object Transition_DetailScreenInactive: MainViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_main_set2
        override val endSetId: Int
            get() = R.id.motion_scene_main_set1
    }
}

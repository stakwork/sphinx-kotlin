package chat.sphinx.splash.ui

import androidx.constraintlayout.motion.widget.MotionLayout
import chat.sphinx.splash.R
import io.matthewnelson.android_concept_views.MotionLayoutViewState

@Suppress("ClassName")
internal sealed class SplashViewState: MotionLayoutViewState<SplashViewState>() {

    object Start_ShowIcon: SplashViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_splash_set1
        override val endSetId: Int
            get() = R.id.motion_scene_splash_set2

        override fun restoreMotionScene(motionLayout: MotionLayout) {}
        override fun transitionToEndSet(motionLayout: MotionLayout) {}
    }

    object Transition_Set2_ShowWelcome: SplashViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_splash_set1
        override val endSetId: Int
            get() = R.id.motion_scene_splash_set2
    }

    object Set2_ShowWelcome: SplashViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_splash_set2
        override val endSetId: Int?
            get() = null

        override fun restoreMotionScene(motionLayout: MotionLayout) {
            motionLayout.setTransition(R.id.transition_splash_set1_to_set2)
            motionLayout.setProgress(1F, 1F)
        }
    }

    class Transition_Set3_DecryptKeys(val toDecrypt: ByteArray): SplashViewState() {

        companion object {
            val START_SET_ID: Int
                get() = R.id.motion_scene_splash_set2
            val END_SET_ID: Int
                get() = R.id.motion_scene_splash_set3
        }

        override val startSetId: Int
            get() = START_SET_ID
        override val endSetId: Int
            get() = END_SET_ID
    }

    class Set3_DecryptKeys(val toDecrypt: ByteArray): SplashViewState() {

        companion object {
            val START_SET_ID: Int
                get() = R.id.motion_scene_splash_set3
            val END_SET_ID: Int
                get() = R.id.motion_scene_splash_set2
        }

        override val startSetId: Int
            get() = START_SET_ID
        override val endSetId: Int
            get() = END_SET_ID

        override fun restoreMotionScene(motionLayout: MotionLayout) {
            motionLayout.setTransition(R.id.transition_splash_set2_to_set3)
            motionLayout.setProgress(1F, 1F)
        }
    }
}
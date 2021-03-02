package chat.sphinx.splash.ui

import androidx.constraintlayout.motion.widget.MotionLayout
import chat.sphinx.splash.R
import io.matthewnelson.android_concept_views.MotionLayoutViewState

internal sealed class SplashViewState: MotionLayoutViewState<SplashViewState>() {

    object Idle: SplashViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_splash_set1
        override val endSetId: Int
            get() = R.id.motion_scene_splash_set2

        override fun restoreMotionScene(motionLayout: MotionLayout) {}
        override fun transitionToEndSet(motionLayout: MotionLayout) {}
    }

    object StartScene: SplashViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_splash_set1
        override val endSetId: Int
            get() = R.id.motion_scene_splash_set2
    }

    object SceneFinished: SplashViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_splash_set2
        override val endSetId: Int?
            get() = null

        override fun restoreMotionScene(motionLayout: MotionLayout) {
            motionLayout.setTransition(R.id.transition_splash_set1_to_set2)
            motionLayout.setProgress(1F, 1F)
        }
    }
}
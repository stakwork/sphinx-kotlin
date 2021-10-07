package chat.sphinx.onboard_connecting.ui

import androidx.constraintlayout.motion.widget.MotionLayout
import chat.sphinx.onboard_common.model.RedemptionCode
import chat.sphinx.onboard_connecting.R
import io.matthewnelson.android_concept_views.MotionLayoutViewState
import java.io.CharArrayWriter

internal sealed class OnBoardConnectingViewState: MotionLayoutViewState<OnBoardConnectingViewState>() {

    object Connecting: OnBoardConnectingViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_connecting_set2
        override val endSetId: Int
            get() = R.id.motion_scene_connecting_set1

        override fun restoreMotionScene(motionLayout: MotionLayout) {}
    }

    class Transition_Set2_DecryptKeys(
        val restoreCode: RedemptionCode.AccountRestoration
    ): OnBoardConnectingViewState() {

        companion object {
            val START_SET_ID: Int
                get() = R.id.motion_scene_connecting_set1
            val END_SET_ID: Int
                get() = R.id.motion_scene_connecting_set2
        }

        override val startSetId: Int
            get() = START_SET_ID
        override val endSetId: Int
            get() = END_SET_ID
    }

    class Set2_DecryptKeys(
        val restoreCode: RedemptionCode.AccountRestoration,
        var inputLock: Boolean = false,
        val pinWriter: CharArrayWriter = CharArrayWriter(6)
    ): OnBoardConnectingViewState() {

        companion object {
            val START_SET_ID: Int
                get() = R.id.motion_scene_connecting_set2
            val END_SET_ID: Int
                get() = R.id.motion_scene_connecting_set1
        }

        override val startSetId: Int
            get() = START_SET_ID
        override val endSetId: Int
            get() = END_SET_ID

        override fun restoreMotionScene(motionLayout: MotionLayout) {
            motionLayout.setTransition(R.id.transition_connecting_set1_to_set2)
            motionLayout.setProgress(1F, 1F)
        }
    }
}
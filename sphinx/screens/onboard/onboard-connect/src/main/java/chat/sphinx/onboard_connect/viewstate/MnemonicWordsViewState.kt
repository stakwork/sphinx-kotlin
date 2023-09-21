package chat.sphinx.onboard_connect.viewstate

import androidx.constraintlayout.motion.widget.MotionLayout
import chat.sphinx.onboard_connect.R
import io.matthewnelson.android_concept_views.MotionLayoutViewState


sealed class MnemonicWordsViewState: MotionLayoutViewState<MnemonicWordsViewState>() {

    object Closed: MnemonicWordsViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_mnemonic_words_open
        override val endSetId: Int?
            get() = R.id.motion_scene_mnemonic_words_closed

        override fun restoreMotionScene(motionLayout: MotionLayout) {}
    }

    object Open: MnemonicWordsViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_mnemonic_words_closed
        override val endSetId: Int?
            get() = R.id.motion_scene_mnemonic_words_open

        override fun restoreMotionScene(motionLayout: MotionLayout) {
            motionLayout.setTransition(R.id.transition_mnemonic_words_closed_to_open)
            motionLayout.setProgress(1F, 1F)
        }
    }
}
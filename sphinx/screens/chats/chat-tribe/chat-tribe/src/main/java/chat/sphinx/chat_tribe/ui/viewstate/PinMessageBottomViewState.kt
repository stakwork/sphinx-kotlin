package chat.sphinx.chat_tribe.ui.viewstate

import androidx.constraintlayout.motion.widget.MotionLayout
import chat.sphinx.chat_tribe.R
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_message.MessageContentDecrypted
import chat.sphinx.wrapper_message.SenderAlias
import io.matthewnelson.android_concept_views.MotionLayoutViewState

sealed class PinMessageBottomViewState: MotionLayoutViewState<PinMessageBottomViewState>() {

    object Closed: PinMessageBottomViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_pin_bottom_open
        override val endSetId: Int
            get() = R.id.motion_scene_pin_bottom_closed

        override fun restoreMotionScene(motionLayout: MotionLayout) {}

    }

    object Open: PinMessageBottomViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_pin_bottom_closed
        override val endSetId: Int
            get() = R.id.motion_scene_pin_bottom_open

        override fun restoreMotionScene(motionLayout: MotionLayout) {
            motionLayout.setTransition(R.id.transition_pin_bottom_closed_to_open)
            motionLayout.setProgress(1F, 1F)
        }

    }
}

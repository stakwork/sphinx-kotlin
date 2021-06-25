package chat.sphinx.chat_common.ui.viewstate

import androidx.constraintlayout.motion.widget.MotionLayout
import chat.sphinx.chat_common.R
import io.matthewnelson.android_concept_views.MotionLayoutViewState

@Suppress("ClassName")
sealed class ActionsMenuViewState: MotionLayoutViewState<ActionsMenuViewState>() {

    object Closed: ActionsMenuViewState() {
        override val startSetId: Int
            get() = -1
        override val endSetId: Int
            get() = -1

        override fun restoreMotionScene(motionLayout: MotionLayout) {}
    }

    object Open: ActionsMenuViewState() {
        override val startSetId: Int
            get() = -1
        override val endSetId: Int
            get() = -1

        override fun restoreMotionScene(motionLayout: MotionLayout) {
//            motionLayout.setTransition(R.id.transition_chat_menu_closed_to_open)
//            motionLayout.setProgress(1F, 1F)
        }
    }
}

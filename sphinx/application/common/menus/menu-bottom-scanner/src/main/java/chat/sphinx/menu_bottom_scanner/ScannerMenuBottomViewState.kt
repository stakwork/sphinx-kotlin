package chat.sphinx.menu_bottom_scanner

import androidx.constraintlayout.motion.widget.MotionLayout
import chat.sphinx.menu_bottom.R
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import io.matthewnelson.android_concept_views.MotionLayoutViewState

sealed class ScannerMenuBottomViewState: MotionLayoutViewState<ScannerMenuBottomViewState>() {

    object Closed: ScannerMenuBottomViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_menu_bottom_open
        override val endSetId: Int
            get() = R.id.motion_scene_menu_bottom_closed

        override fun restoreMotionScene(motionLayout: MotionLayout) {}
    }

    data class Open(val pubKey: LightningNodePubKey, val routeHint: LightningRouteHint?): ScannerMenuBottomViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_menu_bottom_closed
        override val endSetId: Int
            get() = R.id.motion_scene_menu_bottom_open

        override fun restoreMotionScene(motionLayout: MotionLayout) {
            motionLayout.setTransition(R.id.transition_menu_bottom_closed_to_open)
            motionLayout.setProgress(1F, 1F)
        }
    }
}

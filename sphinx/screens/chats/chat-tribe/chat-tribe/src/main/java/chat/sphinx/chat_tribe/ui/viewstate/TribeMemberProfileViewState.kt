package chat.sphinx.chat_tribe.ui.viewstate

import androidx.constraintlayout.motion.widget.MotionLayout
import chat.sphinx.chat_tribe.R
import io.matthewnelson.android_concept_views.MotionLayoutViewState


sealed class TribeMemberProfileViewState: MotionLayoutViewState<TribeMemberProfileViewState>() {

    object Closed: TribeMemberProfileViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_tribe_member_profile_open
        override val endSetId: Int?
            get() = R.id.motion_scene_tribe_member_profile_closed

        override fun restoreMotionScene(motionLayout: MotionLayout) {}
    }

    object Open: TribeMemberProfileViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_tribe_member_profile_closed
        override val endSetId: Int?
            get() = R.id.motion_scene_tribe_member_profile_open

        override fun restoreMotionScene(motionLayout: MotionLayout) {
            motionLayout.setTransition(R.id.transition_tribe_member_profile_closed_to_open)
            motionLayout.setProgress(1F, 1F)
        }
    }

    object FullScreen: TribeMemberProfileViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_tribe_member_profile_open
        override val endSetId: Int?
            get() = R.id.motion_scene_tribe_member_profile_fullscreen

        override fun restoreMotionScene(motionLayout: MotionLayout) {
            motionLayout.setTransition(R.id.transition_tribe_member_profile_open_to_fullscreen)
            motionLayout.setProgress(1F, 1F)
        }
    }
}
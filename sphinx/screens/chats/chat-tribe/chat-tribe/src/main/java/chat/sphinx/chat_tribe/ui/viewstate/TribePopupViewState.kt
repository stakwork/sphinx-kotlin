package chat.sphinx.chat_tribe.ui.viewstate

import androidx.constraintlayout.motion.widget.MotionLayout
import chat.sphinx.chat_tribe.R
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.message.MessageUUID
import chat.sphinx.wrapper_contact.ContactAlias
import chat.sphinx.wrapper_message.SenderAlias
import io.matthewnelson.android_concept_views.MotionLayoutViewState
import io.matthewnelson.concept_views.viewstate.ViewState


sealed class TribePopupViewState: MotionLayoutViewState<TribePopupViewState>() {
    object Idle: TribePopupViewState() {
        override val startSetId: Int
            get() = R.id.motion_scene_tribe_contact_profile_open
        override val endSetId: Int?
            get() = R.id.motion_scene_tribe_contact_profile_closed

        override fun restoreMotionScene(motionLayout: MotionLayout) {}
    }

    class TribeMemberPopup(
        val messageUUID: MessageUUID,
        val memberName: SenderAlias,
        val colorKey: String,
        val memberPic: PhotoUrl?
    ): TribePopupViewState(){
        override val startSetId: Int
            get() = R.id.motion_scene_tribe_contact_profile_closed
        override val endSetId: Int?
            get() = R.id.motion_scene_tribe_contact_profile_open
    }

}
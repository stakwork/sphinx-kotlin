package chat.sphinx.chat_tribe.ui.viewstate

import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.message.MessageUUID
import chat.sphinx.wrapper_contact.ContactAlias
import chat.sphinx.wrapper_message.SenderAlias
import io.matthewnelson.concept_views.viewstate.ViewState


sealed class TribePopupViewState: ViewState<TribePopupViewState>() {
    object Idle: TribePopupViewState()

    class TribeMemberPopup(
        val memberName: SenderAlias,
        val memberPic: PhotoUrl?,
        val messageUUID: MessageUUID
    ): TribePopupViewState()

}
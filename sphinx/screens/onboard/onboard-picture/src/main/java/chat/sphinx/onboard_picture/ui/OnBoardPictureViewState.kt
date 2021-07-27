package chat.sphinx.onboard_picture.ui

import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_contact.ContactAlias
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class OnBoardPictureViewState: ViewState<OnBoardPictureViewState>() {
    object Idle: OnBoardPictureViewState()
    data class UserInfo(
        val name: ContactAlias?,
        val url: PhotoUrl?
    ): OnBoardPictureViewState()
}

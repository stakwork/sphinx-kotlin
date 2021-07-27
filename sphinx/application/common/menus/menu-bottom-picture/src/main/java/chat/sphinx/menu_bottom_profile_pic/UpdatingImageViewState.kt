package chat.sphinx.menu_bottom_profile_pic

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class UpdatingImageViewState: ViewState<UpdatingImageViewState>() {
    object Idle: UpdatingImageViewState()

    object UpdatingImage: UpdatingImageViewState()
    object UpdatingImageFailed: UpdatingImageViewState()
    object UpdatingImageSucceed: UpdatingImageViewState()

}
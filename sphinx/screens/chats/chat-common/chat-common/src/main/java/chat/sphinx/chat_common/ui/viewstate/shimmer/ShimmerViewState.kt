package chat.sphinx.chat_common.ui.viewstate.shimmer

import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_contact.ContactAlias
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class ShimmerViewState: ViewState<ShimmerViewState>() {

    object On: ShimmerViewState()
    object Off: ShimmerViewState()
}

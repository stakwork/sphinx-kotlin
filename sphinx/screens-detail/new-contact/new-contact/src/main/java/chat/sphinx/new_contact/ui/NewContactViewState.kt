package chat.sphinx.new_contact.ui

import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.LightningRouteHint
import io.matthewnelson.concept_views.viewstate.ViewState
import java.io.File

internal sealed class NewContactViewState: ViewState<NewContactViewState>() {
    object Idle: NewContactViewState()
    object Saving: NewContactViewState()
    object Saved: NewContactViewState()
    object Error: NewContactViewState()
}

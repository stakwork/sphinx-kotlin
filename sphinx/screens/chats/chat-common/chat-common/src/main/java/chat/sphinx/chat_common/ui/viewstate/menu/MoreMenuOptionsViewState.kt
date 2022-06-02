package chat.sphinx.chat_common.ui.viewstate.menu

sealed class MoreMenuOptionsViewState {
    object OwnTribe: MoreMenuOptionsViewState()
    object NotOwnTribe: MoreMenuOptionsViewState()
}

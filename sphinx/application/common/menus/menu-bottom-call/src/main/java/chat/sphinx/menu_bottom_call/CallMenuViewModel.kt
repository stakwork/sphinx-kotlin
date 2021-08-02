package chat.sphinx.menu_bottom_call

import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer

interface CallMenuViewModel {
    val callMenuHandler: CallMenuHandler
    val dispatchers: CoroutineDispatchers

    fun sendCallInvite(audioOnly: Boolean)
}

class CallMenuHandler {
    val viewStateContainer: ViewStateContainer<MenuBottomViewState> by lazy {
        ViewStateContainer(MenuBottomViewState.Closed)
    }
}

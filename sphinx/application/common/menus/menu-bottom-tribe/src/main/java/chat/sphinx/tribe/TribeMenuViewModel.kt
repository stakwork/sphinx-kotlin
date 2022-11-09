package chat.sphinx.tribe

import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer

interface TribeMenuViewModel {
    val tribeMenuHandler: TribeMenuHandler
    val dispatchers: CoroutineDispatchers

    fun deleteTribe()
    fun shareTribe()
    fun exitTribe()
    fun editTribe()
    fun addTribeMember()
}

class TribeMenuHandler {
    val viewStateContainer: ViewStateContainer<MenuBottomViewState> by lazy {
        ViewStateContainer(MenuBottomViewState.Closed)
    }
}

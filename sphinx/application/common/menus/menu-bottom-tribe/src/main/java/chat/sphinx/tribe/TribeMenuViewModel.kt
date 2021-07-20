package chat.sphinx.tribe

import android.app.Application
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.CoroutineScope

interface TribeMenuViewModel {
    val tribeMenuHandler: TribeMenuHandler
    val dispatchers: CoroutineDispatchers
}

class TribeMenuHandler(
    private val app: Application,
    private val dispatchers: CoroutineDispatchers,
    private val viewModelScope: CoroutineScope,
) {
    val viewStateContainer: ViewStateContainer<MenuBottomViewState> by lazy {
        ViewStateContainer(MenuBottomViewState.Closed)
    }

    fun deleteTribe() {

    }

    fun shareTribe() {

    }

    fun exitTribe() {

    }

    fun editTribe() {
        TODO("Implement Edit Tribe Functionality on TribeMenuHandler")
    }

}

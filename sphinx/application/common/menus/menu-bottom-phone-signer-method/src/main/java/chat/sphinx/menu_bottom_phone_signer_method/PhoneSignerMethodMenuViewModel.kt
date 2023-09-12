package chat.sphinx.menu_bottom_phone_signer_method

import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer

interface PhoneSignerMethodMenuViewModel {
    val phoneSignerMethodMenuHandler: PhoneSignerMethodMenuHandler
    val dispatchers: CoroutineDispatchers

    fun generateSeed()
    fun importSeed()
}
class PhoneSignerMethodMenuHandler {
    val viewStateContainer: ViewStateContainer<MenuBottomViewState> by lazy {
        ViewStateContainer(MenuBottomViewState.Closed)
    }
}
package chat.sphinx.menu_bottom_signer

import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer

interface SignerMenuViewModel {
    val signerMenuHandler: SignerMenuHandler
    val dispatchers: CoroutineDispatchers

    fun setupHardwareSigner()
    fun setupPhoneSigner()
}
class SignerMenuHandler {
    val viewStateContainer: ViewStateContainer<MenuBottomViewState> by lazy {
        ViewStateContainer(MenuBottomViewState.Closed)
    }
}
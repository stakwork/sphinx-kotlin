package chat.sphinx.menu_bottom_scanner

import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer

interface ScannerMenuViewModel {
    val scannerMenuHandler: ScannerMenuHandler
    val dispatchers: CoroutineDispatchers

    fun createContact()
    fun sendDirectPayment()
    fun scannerMenuDismiss()
}
class ScannerMenuHandler {
    val viewStateContainer: ViewStateContainer<MenuBottomViewState> by lazy {
        ViewStateContainer(MenuBottomViewState.Closed)
    }
}
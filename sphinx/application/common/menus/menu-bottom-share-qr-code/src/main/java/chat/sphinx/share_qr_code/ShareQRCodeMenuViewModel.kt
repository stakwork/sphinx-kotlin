package chat.sphinx.share_qr_code

import android.content.Intent
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer

interface ShareQRCodeMenuViewModel {
    val shareQRCodeMenuHandler: ShareQRCodeMenuHandler
    val dispatchers: CoroutineDispatchers

    fun shareCodeThroughTextIntent(): Intent
    fun shareCodeThroughImageIntent(): Intent?
}

class ShareQRCodeMenuHandler {
    val viewStateContainer: ViewStateContainer<MenuBottomViewState> by lazy {
        ViewStateContainer(MenuBottomViewState.Closed)
    }
}

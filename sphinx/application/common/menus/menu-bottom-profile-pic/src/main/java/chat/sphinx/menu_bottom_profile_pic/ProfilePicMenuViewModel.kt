package chat.sphinx.menu_bottom_profile_pic

import android.net.Uri
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer

interface PictureMenuViewModel {
    val pictureMenuHandler: PictureMenuHandler
    val dispatchers: CoroutineDispatchers

    fun updatePictureFromCamera()

    fun handleActivityResultUri(uri: Uri?)
}

class PictureMenuHandler {
    val viewStateContainer: ViewStateContainer<MenuBottomViewState> by lazy {
        ViewStateContainer(MenuBottomViewState.Closed)
    }
}

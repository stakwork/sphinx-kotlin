package chat.sphinx.menu_bottom_tribe_pic

import android.net.Uri
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer

interface TribePicMenuViewModel {
    val tribePicMenuHandler: TribePicMenuHandler
    val dispatchers: CoroutineDispatchers

    fun updateProfilePicCamera()

    fun handleActivityResultUri(uri: Uri?)
}

class TribePicMenuHandler {
    val viewStateContainer: ViewStateContainer<MenuBottomViewState> by lazy {
        ViewStateContainer(MenuBottomViewState.Closed)
    }
}

package chat.sphinx.menu_bottom_tribe_profile_pic

import android.net.Uri
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer

interface TribeProfilePicMenuViewModel {
    val tribeProfilePicMenuHandler: TribeProfilePicMenuHandler
    val dispatchers: CoroutineDispatchers

    fun updateProfilePicCamera()

    fun handleActivityResultUri(uri: Uri?)
}

class TribeProfilePicMenuHandler {
    val viewStateContainer: ViewStateContainer<MenuBottomViewState> by lazy {
        ViewStateContainer(MenuBottomViewState.Closed)
    }
}

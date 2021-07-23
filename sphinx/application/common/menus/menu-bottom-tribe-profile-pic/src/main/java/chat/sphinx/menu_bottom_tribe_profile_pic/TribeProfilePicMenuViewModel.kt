package chat.sphinx.menu_bottom_tribe_profile_pic

import android.net.Uri
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer

@Deprecated(message = "Do Not Use. Incorrect duplication of ProfilePicMenu")
interface TribeProfilePicMenuViewModel {
    val tribeProfilePicMenuHandler: TribeProfilePicMenuHandler
    val dispatchers: CoroutineDispatchers

    fun updateChatProfilePicCamera()

    fun handleActivityResultUri(uri: Uri?)
}

@Deprecated(message = "Do Not Use. Incorrect duplication of ProfilePicMenu")
class TribeProfilePicMenuHandler {
    val viewStateContainer: ViewStateContainer<MenuBottomViewState> by lazy {
        ViewStateContainer(MenuBottomViewState.Closed)
    }
}

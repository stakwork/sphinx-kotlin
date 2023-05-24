package chat.sphinx.example.delete_media.ui

import android.app.Application
import android.content.*
import androidx.lifecycle.SavedStateHandle
import chat.sphinx.concept_socket_io.SocketIOManager
import chat.sphinx.example.delete_media.navigation.DeleteMediaNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import javax.inject.Inject

@HiltViewModel
internal class DeleteMediaViewModel @Inject constructor(
    private val app: Application,
    val navigator: DeleteMediaNavigator,
    private val socketIOManager: SocketIOManager,
    private val mediaCacheHandler: MediaCacheHandler,
    dispatchers: CoroutineDispatchers,
    handle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        DeleteNotifySideEffect,
        DeleteMediaViewState
        >(dispatchers, DeleteMediaViewState.Idle)
{

}

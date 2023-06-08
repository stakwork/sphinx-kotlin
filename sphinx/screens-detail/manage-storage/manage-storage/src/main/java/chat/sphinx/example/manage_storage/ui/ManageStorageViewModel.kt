package chat.sphinx.example.manage_storage.ui

import android.app.Application
import android.content.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_socket_io.SocketIOManager
import chat.sphinx.example.manage_storage.navigation.ManageStorageNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class ManageStorageViewModel @Inject constructor(
    private val app: Application,
    val navigator: ManageStorageNavigator,
    private val repositoryMedia: RepositoryMedia,
    dispatchers: CoroutineDispatchers,
    handle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        StorageNotifySideEffect,
        ManageStorageViewState
        >(dispatchers, ManageStorageViewState.Idle)
{
    val changeStorageLimitViewStateContainer: ViewStateContainer<ChangeStorageLimitViewState> by lazy {
        ViewStateContainer(ChangeStorageLimitViewState.Closed)
    }

    init {
        viewModelScope.launch(mainImmediate) {
            repositoryMedia.getStorageDataInfo().collect {}
        }
    }
}

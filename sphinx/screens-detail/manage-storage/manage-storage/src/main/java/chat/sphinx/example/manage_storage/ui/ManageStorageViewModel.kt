package chat.sphinx.example.manage_storage.ui

import android.app.Application
import android.content.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.example.manage_storage.model.StorageSize
import chat.sphinx.wrapper_common.calculateStoragePercentage
import chat.sphinx.example.manage_storage.navigation.ManageStorageNavigator
import chat.sphinx.example.manage_storage.viewstate.DeleteTypeNotificationViewState
import chat.sphinx.example.manage_storage.viewstate.ManageStorageViewState
import chat.sphinx.manage.storage.R
import chat.sphinx.wrapper_common.calculateSize
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
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
        >(dispatchers, ManageStorageViewState.Loading)
{
    val changeStorageLimitViewStateContainer: ViewStateContainer<ChangeStorageLimitViewState> by lazy {
        ViewStateContainer(ChangeStorageLimitViewState.Closed)
    }

    val deleteItemNotificationViewStateContainer: ViewStateContainer<DeleteTypeNotificationViewState> by lazy {
        ViewStateContainer(DeleteTypeNotificationViewState.Closed)
    }

    init {
        getStorageData()
    }

    fun getStorageData(){
        viewModelScope.launch(mainImmediate) {
            repositoryMedia.getStorageDataInfo().collect { storageData ->
                val storageSize = StorageSize(
                    storageData.usedStorage.calculateSize(),
                    storageData.freeStorage.calculateSize(),
                    storageData.images.calculateSize(),
                    storageData.video.calculateSize(),
                    storageData.audio.calculateSize(),
                    storageData.files.calculateSize(),
                    storageData.chats.calculateSize(),
                    storageData.podcasts.calculateSize()
                )
                val storagePercentage = calculateStoragePercentage(storageData)

                updateViewState(ManageStorageViewState.StorageInfo(storageSize, storagePercentage))
            }
        }
    }

    fun openDeleteTypePopUp(type: String) {
        deleteItemNotificationViewStateContainer.updateViewState(DeleteTypeNotificationViewState.Open(type))
    }

    fun featureNotImplementedToast(){
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(
                StorageNotifySideEffect(app.getString(R.string.manage_storage_delete_feature_not_implemented))
            )
        }
    }
}

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
import chat.sphinx.wrapper_common.StorageData
import chat.sphinx.wrapper_common.calculateSize
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.message.MessageId
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
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
    companion object {
        const val IMAGE_TYPE = "Images"
        const val VIDEO_TYPE = "Videos"
        const val AUDIO_TYPE = "Audios"
        const val FILE_TYPE = "Files"
    }

    private var storageData: StorageData? = null

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
            repositoryMedia.getStorageDataInfo().collect { storageDataInfo ->
                storageData = storageDataInfo
                val storageSize = StorageSize(
                    storageDataInfo.usedStorage.calculateSize(),
                    storageDataInfo.freeStorage.calculateSize(),
                    storageDataInfo.images.totalSize.calculateSize(),
                    storageDataInfo.video.totalSize.calculateSize(),
                    storageDataInfo.audio.totalSize.calculateSize(),
                    storageDataInfo.files.totalSize.calculateSize(),
                    storageDataInfo.chatsStorage.calculateSize(),
                    storageDataInfo.podcastsStorage.calculateSize()
                )
                val storagePercentage = calculateStoragePercentage(storageDataInfo)

                updateViewState(ManageStorageViewState.StorageInfo(storageSize, storagePercentage))
            }
        }
    }

    fun openDeleteTypePopUp(type: String) {
        deleteItemNotificationViewStateContainer.updateViewState(DeleteTypeNotificationViewState.Open(type))
    }

    fun deleteAllFilesByType(type: String) {
        when (type) {
            IMAGE_TYPE -> {
                storageData?.images.let { imageStorage ->
                    imageStorage?.items?.keys?.forEach { chatId ->
                        deleteDownloadedMedia(chatId, imageStorage.fileList, imageStorage.items[chatId])
                    }
                }
            }
            VIDEO_TYPE -> {
                storageData?.video.let { videoStorage ->
                    videoStorage?.items?.keys?.forEach { chatId ->
                        deleteDownloadedMedia(chatId, videoStorage.fileList, videoStorage.items[chatId])
                    }
                }
            }
            AUDIO_TYPE -> {
                storageData?.audio.let { audioStorage ->
                    audioStorage?.chatItems?.keys?.forEach { chatId ->
                        deleteDownloadedMedia(chatId, audioStorage.fileList, audioStorage.chatItems[chatId])
                    }
                }
            }
            FILE_TYPE -> {
                storageData?.files.let { filesStorage ->
                    filesStorage?.items?.keys?.forEach { chatId ->
                        deleteDownloadedMedia(chatId, filesStorage.fileList, filesStorage.items[chatId])
                    }
                }
            }
        }
    }

    private fun deleteDownloadedMedia(chatId: ChatId, files: List<File>, messageIds: List<MessageId>?) {
        viewModelScope.launch(mainImmediate) {
            val deleteResponse = repositoryMedia.deleteDownloadedMediaByChatId(
                chatId,
                files,
                messageIds
            )
            if (deleteResponse) {
                deleteItemNotificationViewStateContainer.updateViewState(DeleteTypeNotificationViewState.Closed)
            } else {
                submitSideEffect(
                    StorageNotifySideEffect(app.getString(R.string.manage_storage_error_delete))
                )
                deleteItemNotificationViewStateContainer.updateViewState(DeleteTypeNotificationViewState.Closed)
            }
        }
    }

    fun featureNotImplementedToast(){
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(
                StorageNotifySideEffect(app.getString(R.string.manage_storage_delete_feature_not_implemented))
            )
        }
    }
}

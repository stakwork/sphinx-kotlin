package chat.sphinx.example.manage_storage.ui

import android.app.Application
import android.content.*
import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.example.manage_storage.model.StorageLimit
import chat.sphinx.example.manage_storage.model.StorageSize
import chat.sphinx.wrapper_common.calculateStoragePercentage
import chat.sphinx.example.manage_storage.navigation.ManageStorageNavigator
import chat.sphinx.example.manage_storage.viewstate.DeleteTypeNotificationViewState
import chat.sphinx.example.manage_storage.viewstate.ManageStorageViewState
import chat.sphinx.manage.storage.R
import chat.sphinx.wrapper_common.StorageData
import chat.sphinx.wrapper_common.StorageLimit.DEFAULT_STORAGE_LIMIT
import chat.sphinx.wrapper_common.StorageLimit.STORAGE_LIMIT_KEY
import chat.sphinx.wrapper_common.calculateSize
import chat.sphinx.wrapper_common.calculateUsedStoragePercentage
import chat.sphinx.wrapper_common.calculateUserStorageLimit
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.toFileSize
import chat.sphinx.wrapper_feed.FeedItem
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
internal class ManageStorageViewModel @Inject constructor(
    private val app: Application,
    val navigator: ManageStorageNavigator,
    private val repositoryMedia: RepositoryMedia,
    private val feedRepository: FeedRepository,
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

    private val _storageDataStateFlow: MutableStateFlow<StorageData?> by lazy {
        MutableStateFlow(null)
    }
    private val storageDataStateFlow: StateFlow<StorageData?>
        get() = _storageDataStateFlow.asStateFlow()

    val changeStorageLimitViewStateContainer: ViewStateContainer<ChangeStorageLimitViewState> by lazy {
        ViewStateContainer(ChangeStorageLimitViewState.Closed)
    }

    val deleteItemNotificationViewStateContainer: ViewStateContainer<DeleteTypeNotificationViewState> by lazy {
        ViewStateContainer(DeleteTypeNotificationViewState.Closed)
    }

    private val storageLimitSharedPreferences: SharedPreferences =
        app.applicationContext.getSharedPreferences(STORAGE_LIMIT_KEY, Context.MODE_PRIVATE)

    init {
        getStorageData()
    }

    private fun getStorageData(){
        viewModelScope.launch(mainImmediate) {
            repositoryMedia.getStorageDataInfo().collect { storageDataInfo ->

                val totalStorage = getUserFreeStorage()
                val usedStorage = storageDataInfo.usedStorage
                val freeStorage = (totalStorage - usedStorage.value).toFileSize()
                val modifiedStorageDataInfo = storageDataInfo.copy(freeStorage = freeStorage)
                _storageDataStateFlow.value = modifiedStorageDataInfo

                val storageSize = StorageSize(
                    storageDataInfo.usedStorage.calculateSize(),
                    freeStorage?.calculateSize(),
                    storageDataInfo.images.totalSize.calculateSize(),
                    storageDataInfo.video.totalSize.calculateSize(),
                    storageDataInfo.audio.totalSize.calculateSize(),
                    storageDataInfo.files.totalSize.calculateSize(),
                    storageDataInfo.chatsStorage.calculateSize(),
                    storageDataInfo.podcastsStorage.calculateSize()
                )

                val storagePercentage = calculateStoragePercentage(modifiedStorageDataInfo)

                updateViewState(ManageStorageViewState.StorageInfo(storageSize, storagePercentage))
            }
        }
    }

    private fun getDeleteExcessFileIfApplicable(){
        viewModelScope.launch(mainImmediate) {
            storageDataStateFlow.value?.let { nnStorageData ->

                val storageLimitProgress = storageLimitSharedPreferences.getInt(STORAGE_LIMIT_KEY, DEFAULT_STORAGE_LIMIT)
                val userLimit = nnStorageData.freeStorage?.value?.let { calculateUserStorageLimit(freeStorage = it, seekBarValue = storageLimitProgress ) } ?: 0L
                val usageStorage = nnStorageData.usedStorage.value
                val excessSize = (usageStorage - userLimit)

                repositoryMedia.deleteExcessFilesOnBackground(excessSize)
            }
        }
    }

    fun openDeleteTypePopUp(type: String) {
        deleteItemNotificationViewStateContainer.updateViewState(DeleteTypeNotificationViewState.Open(type))
    }

    fun deleteAllFilesByType(type: String) {
        when (type) {
            IMAGE_TYPE -> {
                storageDataStateFlow.value?.images?.let { imageStorage ->
                    imageStorage.items.keys.forEach { chatId ->
                        deleteDownloadedMedia(chatId, imageStorage.fileList, imageStorage.items[chatId])
                    }
                }
            }
            VIDEO_TYPE -> {
                storageDataStateFlow.value?.video?.let { videoStorage ->
                    videoStorage.items.keys.forEach { chatId ->
                        deleteDownloadedMedia(chatId, videoStorage.fileList, videoStorage.items[chatId])
                    }
                }
            }
            AUDIO_TYPE -> {
                storageDataStateFlow.value?.audio?.let { audioStorage ->
                    deleteAllDownloadedFeeds(audioStorage.feedList)
                    audioStorage.chatItems.keys.forEach { chatId ->
                        deleteDownloadedMedia(chatId, audioStorage.fileList, audioStorage.chatItems[chatId])
                    }
                }
            }
            FILE_TYPE -> {
                storageDataStateFlow.value?.files?.let { filesStorage ->
                    filesStorage.items.keys.forEach { chatId ->
                        deleteDownloadedMedia(chatId, filesStorage.fileList, filesStorage.items[chatId])
                    }
                }
            }
        }
    }

    private fun deleteDownloadedMedia(chatId: ChatId, files: List<File>, messageIds: List<MessageId>?) {
        deleteItemNotificationViewStateContainer.updateViewState(DeleteTypeNotificationViewState.Closed)

        viewModelScope.launch(mainImmediate) {
            val deleteResponse = repositoryMedia.deleteDownloadedMediaByChatId(
                chatId,
                files,
                messageIds
            )

            if (!deleteResponse) {
                submitSideEffect(
                    StorageNotifySideEffect(app.getString(R.string.manage_storage_error_delete))
                )
                deleteItemNotificationViewStateContainer.updateViewState(DeleteTypeNotificationViewState.Closed)
            }
        }
    }

    private fun deleteAllDownloadedFeeds(feedsIds: List<FeedId>) {
        viewModelScope.launch(mainImmediate) {
            feedsIds.forEach { feedId ->
                feedId.let { nnFeedId ->
                    feedRepository.getFeedById(nnFeedId).firstOrNull()?.let {nnFeed ->

                        repositoryMedia.deleteAllFeedDownloadedMedia(nnFeed)
                    }
                }
            }
        }
    }

    fun retrieveStorageLimitFromPreferences() {
        val storageLimitProgress = storageLimitSharedPreferences.getInt(STORAGE_LIMIT_KEY, DEFAULT_STORAGE_LIMIT)
        updateStorageLimitViewState(storageLimitProgress)
    }

    fun updateStorageLimitViewState(progress: Int) {
        storageDataStateFlow.value?.let { nnStorageData ->

            val usedStorage: Long = nnStorageData.usedStorage.value
            val freeStorage = nnStorageData.freeStorage?.value ?: 0L
            val userStorageLimit: Long = calculateUserStorageLimit(progress, freeStorage)
            val progressBarPercentage = calculateUsedStoragePercentage(nnStorageData)

            val undersized: String? =
                if (userStorageLimit < usedStorage) {
                    (usedStorage - userStorageLimit).toFileSize()?.calculateSize()
                } else null

            changeStorageLimitViewStateContainer.updateViewState(
                ChangeStorageLimitViewState.Open(
                    StorageLimit(
                        progress,
                        usedStorage.toFileSize()?.calculateSize(),
                        userStorageLimit.toFileSize()?.calculateSize(),
                        freeStorage.toFileSize()?.calculateSize(),
                        undersized,
                        progressBarPercentage
                    )
                )
            )
        }
    }

    fun setStorageLimit(progress: Int) {
        val editor = storageLimitSharedPreferences.edit()
        editor.putInt(STORAGE_LIMIT_KEY, progress)
        editor.apply()

        changeStorageLimitViewStateContainer.updateViewState(ChangeStorageLimitViewState.Closed)

        getDeleteExcessFileIfApplicable()
    }

    private fun getUserFreeStorage(): Long {
        val stat = StatFs(Environment.getDataDirectory().path)
        return stat.blockSizeLong * stat.availableBlocksLong
    }

}

package chat.sphinx.example.delete_media_detail.ui

import android.app.Application
import android.content.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.delete.media.detail.R
import chat.sphinx.example.delete_media_detail.model.PodcastDetailToDelete
import chat.sphinx.example.delete_media_detail.navigation.DeleteMediaDetailNavigator
import chat.sphinx.example.delete_media_detail.viewstate.DeleteAllNotificationViewStateContainer
import chat.sphinx.example.delete_media_detail.viewstate.DeleteMediaDetailViewState
import chat.sphinx.example.delete_media_detail.viewstate.DeleteItemNotificationViewState
import chat.sphinx.wrapper_common.FileSize
import chat.sphinx.wrapper_common.calculateSize
import chat.sphinx.wrapper_common.calculateTotalSize
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedItem
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class DeletePodcastDetailViewModel @Inject constructor(
    private val app: Application,
    val navigator: DeleteMediaDetailNavigator,
    private val feedRepository: FeedRepository,
    private val repositoryMedia: RepositoryMedia,
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        DeleteDetailNotifySideEffect,
        DeleteMediaDetailViewState
        >(dispatchers, DeleteMediaDetailViewState.Idle)
{
    private val args: DeletePodcastDetailFragmentArgs by savedStateHandle.navArgs()
    private var currentFeed: Feed? = null
    private var itemsTotalSize: String = ""

    val deleteAllNotificationViewStateContainer: ViewStateContainer<DeleteAllNotificationViewStateContainer> by lazy {
        ViewStateContainer(DeleteAllNotificationViewStateContainer.Closed)
    }

    val deleteItemNotificationViewStateContainer: ViewStateContainer<DeleteItemNotificationViewState> by lazy {
        ViewStateContainer(DeleteItemNotificationViewState.Closed)
    }

    init {
        viewModelScope.launch(mainImmediate) {
            feedRepository.getDownloadedFeedItemsByFeedId(FeedId(args.argFeedId)).collect { feedItemList ->
                val feed = feedRepository.getFeedById(FeedId(args.argFeedId)).firstOrNull()
                val totalSize = feedItemList.map { FileSize(it.localFile?.length() ?: 0L) }
                currentFeed = feed
                setItemTotalFile(totalSize)
                val podcastDetailToDeleteLists: List<PodcastDetailToDelete> = feedItemList.sortedByDescending { it.datePublishedTime }.map { feedItem ->
                    PodcastDetailToDelete(
                        feedItem,
                        FileSize(feedItem.localFile?.length() ?: 0L).calculateSize()
                    )
                }
                updateViewState(DeleteMediaDetailViewState.EpisodeList(feed?.titleToShow ?: "", totalSize.calculateTotalSize(), podcastDetailToDeleteLists))
            }
        }
    }

    fun deleteDownloadedFeedItem(feedItem: FeedItem) {
            viewModelScope.launch(mainImmediate) {
                if (repositoryMedia.deleteDownloadedMediaIfApplicable(feedItem)) {
                    deleteItemNotificationViewStateContainer.updateViewState(DeleteItemNotificationViewState.Closed)
                }
                else {
                    deleteItemNotificationViewStateContainer.updateViewState(DeleteItemNotificationViewState.Closed)
                        submitSideEffect(
                            DeleteDetailNotifySideEffect(app.getString(R.string.manage_storage_error_delete))
                        )
                }
            }
        }

    fun deleteAllDownloadedFeedItems() {
        deleteAllNotificationViewStateContainer.updateViewState(DeleteAllNotificationViewStateContainer.Deleting)
        viewModelScope.launch(mainImmediate) {
            currentFeed?.let { nnFeed ->
                if (repositoryMedia.deleteAllFeedDownloadedMedia(nnFeed)) {
                    deleteAllNotificationViewStateContainer.updateViewState(DeleteAllNotificationViewStateContainer.Deleted(itemsTotalSize))
                }
                else {
                    deleteAllNotificationViewStateContainer.updateViewState(DeleteAllNotificationViewStateContainer.Closed)
                    submitSideEffect(
                        DeleteDetailNotifySideEffect(app.getString(R.string.manage_storage_error_delete))
                    )
                }
            }
        }
    }

    private fun setItemTotalFile(files: List<FileSize>) {
        val totalSize = files.sumOf { it.value }
        if (totalSize > 0L) {
            itemsTotalSize = files.calculateTotalSize()
        }
    }

    fun openDeleteItemPopup(feedItem: FeedItem) {
        deleteItemNotificationViewStateContainer.updateViewState(DeleteItemNotificationViewState.Open(feedItem))
    }
    fun closeDeleteItemPopup() {
        deleteItemNotificationViewStateContainer.updateViewState(DeleteItemNotificationViewState.Closed)
    }


}

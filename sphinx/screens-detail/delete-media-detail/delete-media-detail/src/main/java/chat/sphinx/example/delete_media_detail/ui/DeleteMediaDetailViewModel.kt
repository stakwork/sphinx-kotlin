package chat.sphinx.example.delete_media_detail.ui

import android.app.Application
import android.content.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.example.delete_media_detail.model.EpisodeToDelete
import chat.sphinx.example.delete_media_detail.navigation.DeleteMediaDetailNavigator
import chat.sphinx.example.delete_media_detail.viewstate.DeleteMediaDetailViewState
import chat.sphinx.example.delete_media_detail.viewstate.DeleteNotificationViewState
import chat.sphinx.wrapper_common.FileSize
import chat.sphinx.wrapper_common.calculateSize
import chat.sphinx.wrapper_common.calculateTotalSize
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.toFeedId
import chat.sphinx.wrapper_feed.Feed
import chat.sphinx.wrapper_feed.FeedItem
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import java.text.DecimalFormat
import javax.inject.Inject

@HiltViewModel
internal class DeleteMediaDetailViewModel @Inject constructor(
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

    private val args: DeleteMediaDetailFragmentArgs by savedStateHandle.navArgs()
    private var currentFeed: Feed? = null

    init {
        viewModelScope.launch(mainImmediate) {
            feedRepository.getDownloadedFeedItemsByFeedId(FeedId(args.argFeedId)).collect { feedItemList ->
                val feed = feedRepository.getFeedById(FeedId(args.argFeedId)).firstOrNull()
                currentFeed = feed
                val totalSize = feedItemList.map { FileSize(it.localFile?.length() ?: 0L) }.calculateTotalSize()
                val episodeToDeleteList: List<EpisodeToDelete> = feedItemList.map { feedItem ->
                    EpisodeToDelete(
                        feedItem,
                        FileSize(feedItem.localFile?.length() ?: 0L).calculateSize()
                    )
                }
                updateViewState(DeleteMediaDetailViewState.EpisodeList(feed?.titleToShow ?: "", totalSize, episodeToDeleteList))
            }

        }
    }

    fun deleteDownloadedMedia(feedItem: FeedItem) {
            viewModelScope.launch(mainImmediate) {
                repositoryMedia.deleteDownloadedMediaIfApplicable(feedItem)
            }
        }

    fun deleteAllFeedItems() {
        viewModelScope.launch(mainImmediate) {
            currentFeed?.let { nnFeed ->
                repositoryMedia.deleteAllFeedDownloadedMedia(nnFeed)
            }
        }
    }

    fun openDeleteItemPopUp() {
        deleteNotificationViewStateContainer.updateViewState(DeleteNotificationViewState.Open)
    }
    fun closeDeleteItemPopUp() {
        deleteNotificationViewStateContainer.updateViewState(DeleteNotificationViewState.Closed)
    }


    val deleteNotificationViewStateContainer: ViewStateContainer<DeleteNotificationViewState> by lazy {
        ViewStateContainer(DeleteNotificationViewState.Closed)
    }

}

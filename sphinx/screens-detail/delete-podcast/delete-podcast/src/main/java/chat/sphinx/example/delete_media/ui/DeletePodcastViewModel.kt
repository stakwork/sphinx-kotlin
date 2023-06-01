package chat.sphinx.example.delete_media.ui

import android.app.Application
import android.content.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.example.delete_media.model.PodcastToDelete
import chat.sphinx.example.delete_media.navigation.DeleteMediaNavigator
import chat.sphinx.example.delete_media.viewstate.DeletePodcastViewState
import chat.sphinx.example.delete_media.viewstate.DeleteNotificationViewState
import chat.sphinx.wrapper_common.FileSize
import chat.sphinx.wrapper_common.calculateSize
import chat.sphinx.wrapper_common.calculateTotalSize
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.toFileSize
import chat.sphinx.wrapper_feed.FeedItem
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
internal class DeletePodcastViewModel @Inject constructor(
    private val app: Application,
    val navigator: DeleteMediaNavigator,
    private val feedRepository: FeedRepository,
    private val repositoryMedia: RepositoryMedia,
    dispatchers: CoroutineDispatchers,
    handle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        DeleteNotifySideEffect,
        DeletePodcastViewState
        >(dispatchers, DeletePodcastViewState.Loading)
{
     val deleteAllFeedsNotificationViewStateContainer: ViewStateContainer<DeleteNotificationViewState> by lazy {
        ViewStateContainer(DeleteNotificationViewState.Closed)
    }
    private var feedIdsList: List<FeedId?>? = null

    init {
        viewModelScope.launch(mainImmediate) {
            feedRepository.getAllDownloadedFeedItems().collect { feedItems ->
                val feedIdAndFileList = getLocalFilesGroupedByFeed(feedItems)
                val totalSizeAllSections: String? = feedItems.sumOf { it.localFile?.length() ?: 0 }.toFileSize()?.calculateSize()
                feedIdsList = feedIdAndFileList.map { it.key }

                    feedIdAndFileList.keys.mapNotNull { feedId ->
                    val podcast = feedId?.let { feedRepository.getPodcastById(it).firstOrNull() }
                    val listOfFiles = feedIdAndFileList[feedId]

                    if (podcast != null && listOfFiles != null) {
                        val totalSize = listOfFiles.map { FileSize(it.length()) }.calculateTotalSize()
                        PodcastToDelete(
                            podcast.title.value,
                            podcast.imageToShow?.value.orEmpty(),
                            totalSize,
                            podcast.id
                        )
                    } else null
                }.also { sectionList ->
                    viewStateContainer.updateViewState(DeletePodcastViewState.SectionList(sectionList, totalSizeAllSections))
                }
            }
        }
    }

    fun deleteAllDownloadedFeeds() {
        deleteAllFeedsNotificationViewStateContainer.updateViewState(DeleteNotificationViewState.Deleting)
        viewModelScope.launch(mainImmediate) {
            feedIdsList?.forEach { feedId ->
                feedId?.let { nnFeedId ->
                    feedRepository.getFeedById(nnFeedId).firstOrNull()?.let {nnFeed ->
                        repositoryMedia.deleteAllFeedDownloadedMedia(nnFeed)
                    }
                }
            }
            deleteAllFeedsNotificationViewStateContainer.updateViewState(DeleteNotificationViewState.SuccessfullyDeleted)
        }
    }


    private fun getLocalFilesGroupedByFeed(feedItems: List<FeedItem>): Map<FeedId?, List<File>> {
        return feedItems.groupBy({ it.feedId }, { it.localFile as File })
    }



}

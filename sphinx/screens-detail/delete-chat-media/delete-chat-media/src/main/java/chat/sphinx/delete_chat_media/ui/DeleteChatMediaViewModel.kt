package chat.sphinx.delete_chat_media.ui

import android.app.Application
import android.content.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.delete_chat_media.navigation.DeleteChatMediaNavigator
import chat.sphinx.delete_chat_media.viewstate.DeleteChatMediaViewState
import chat.sphinx.delete_chat_media.viewstate.DeleteChatNotificationViewState
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_feed.FeedItem
import chat.sphinx.wrapper_message_media.MessageMedia
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
internal class DeleteChatMediaViewModel @Inject constructor(
    private val app: Application,
    val navigator: DeleteChatMediaNavigator,
    private val feedRepository: FeedRepository,
    private val repositoryMedia: RepositoryMedia,
    dispatchers: CoroutineDispatchers,
    handle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        DeleteNotifySideEffect,
        DeleteChatMediaViewState
        >(dispatchers, DeleteChatMediaViewState.Loading)
{
     val deleteAllFeedsNotificationViewStateContainer: ViewStateContainer<DeleteChatNotificationViewState> by lazy {
        ViewStateContainer(DeleteChatNotificationViewState.Closed)
    }

    init {
        viewModelScope.launch(mainImmediate) {
            repositoryMedia.getAllDownloadedMedia().collect { chatItems ->
                val pim = chatItems

//                val feedIdAndFileList = getLocalFilesGroupedByFeed(feedItems)
//                val totalSizeAllSections = feedItems.sumOf { it.localFile?.length() ?: 0 }.toFileSize()
//                setItemTotalFile(totalSizeAllSections?.value ?: 0L )
//                feedIdsList = feedIdAndFileList.map { it.key }
//
//                    feedIdAndFileList.keys.mapNotNull { feedId ->
//                    val podcast = feedId?.let { feedRepository.getPodcastById(it).firstOrNull() }
//                    val listOfFiles = feedIdAndFileList[feedId]
//
//                    if (podcast != null && listOfFiles != null) {
//                        val totalSize = listOfFiles.map { FileSize(it.length()) }.calculateTotalSize()
//                        PodcastToDelete(
//                            podcast.title.value,
//                            podcast.imageToShow?.value.orEmpty(),
//                            totalSize,
//                            podcast.id
//                        )
//                    } else null
//                }.also { sectionList ->
//                    viewStateContainer.updateViewState(DeletePodcastViewState.SectionList(sectionList, totalSizeAllSections?.calculateSize()))
//                }
            }
        }
    }

//    fun deleteAllDownloadedFeeds() {
//        deleteAllFeedsNotificationViewStateContainer.updateViewState(DeleteNotificationViewState.Deleting)
//        viewModelScope.launch(mainImmediate) {
//            feedIdsList?.forEach { feedId ->
//                feedId?.let { nnFeedId ->
//                    feedRepository.getFeedById(nnFeedId).firstOrNull()?.let {nnFeed ->
//                        repositoryMedia.deleteAllFeedDownloadedMedia(nnFeed)
//                    }
//                }
//            }
//            deleteAllFeedsNotificationViewStateContainer.updateViewState(DeleteNotificationViewState.SuccessfullyDeleted(itemsTotalSize.calculateSize()))
//        }
//    }

//    private fun setItemTotalFile(totalSize: Long) {
//        if (totalSize > 0L && totalSize >= itemsTotalSize.value) {
//            itemsTotalSize = FileSize(totalSize)
//        }
//    }

    private fun getLocalFilesGroupedByChatId(feedItems: List<MessageMedia>): Map<FeedId?, List<File>> {
        return feedItems.groupBy({ it.localFile }, { it.localFile as File })
    }



}

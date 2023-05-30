package chat.sphinx.example.delete_media.ui

import android.app.Application
import android.content.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.example.delete_media.model.MediaSection
import chat.sphinx.example.delete_media.navigation.DeleteMediaNavigator
import chat.sphinx.example.delete_media.viewstate.DeleteMediaViewState
import chat.sphinx.example.delete_media.viewstate.DeleteNotificationViewState
import chat.sphinx.wrapper_common.FileSize
import chat.sphinx.wrapper_common.calculateTotalSize
import chat.sphinx.wrapper_common.feed.FeedId
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
internal class DeleteMediaViewModel @Inject constructor(
    private val app: Application,
    val navigator: DeleteMediaNavigator,
    private val feedRepository: FeedRepository,
    private val repositoryMedia: RepositoryMedia,
    dispatchers: CoroutineDispatchers,
    handle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        DeleteNotifySideEffect,
        DeleteMediaViewState
        >(dispatchers, DeleteMediaViewState.Loading)
{
    val deleteNotificationViewStateContainer: ViewStateContainer<DeleteNotificationViewState> by lazy {
        ViewStateContainer(DeleteNotificationViewState.Closed)
    }

    init {
        viewModelScope.launch(mainImmediate) {
            feedRepository.getAllDownloadedFeedItems().collect { feedItems ->
                val feedList = getLocalFilesGroupedByFeed(feedItems)

                feedList.keys.mapNotNull { feedId ->
                    val podcast = feedId?.let { feedRepository.getPodcastById(it).firstOrNull() }
                    val listOfFiles = feedList[feedId]

                    if (podcast != null && listOfFiles != null) {
                        val totalSize = listOfFiles.map { FileSize(it.length()) }.calculateTotalSize()
                        MediaSection(
                            podcast.title.value,
                            podcast.imageToShow?.value.orEmpty(),
                            totalSize,
                            podcast.id
                        )
                    } else null
                }.also { sectionList ->
                    viewStateContainer.updateViewState(DeleteMediaViewState.SectionList(sectionList))
                }
            }
        }
    }

    private fun getLocalFilesGroupedByFeed(feedItems: List<FeedItem>): Map<FeedId?, List<File>> {
        return feedItems.groupBy({ it.feedId }, { it.localFile as File })
    }


}

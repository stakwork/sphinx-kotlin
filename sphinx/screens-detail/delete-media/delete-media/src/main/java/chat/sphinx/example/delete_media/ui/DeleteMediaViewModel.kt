package chat.sphinx.example.delete_media.ui

import android.app.Application
import android.content.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.example.delete_media.model.MediaSection
import chat.sphinx.example.delete_media.navigation.DeleteMediaNavigator
import chat.sphinx.example.delete_media.viewstate.DeleteMediaViewState
import chat.sphinx.example.delete_media.viewstate.DeleteNotificationViewState
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_feed.FeedItem
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import java.text.DecimalFormat
import javax.inject.Inject

@HiltViewModel
internal class DeleteMediaViewModel @Inject constructor(
    private val app: Application,
    val navigator: DeleteMediaNavigator,
    private val feedRepository: FeedRepository,
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
                val feedItemDownloadedList = feedItems
                val feedList = getLocalFilesGroupedByFeed(feedItemDownloadedList)
                val sectionList = mutableListOf<MediaSection>()

                feedList.keys.forEach { feedId ->
                    feedId?.let { nnFeedId ->
                        val podcast = feedRepository.getPodcastById(nnFeedId).firstOrNull()
                        val listOfFiles = feedList[nnFeedId]
                        listOfFiles?.let { nnListOfFiles ->
                            podcast?.let { podcast ->
                                sectionList.add(
                                MediaSection(
                                    podcast.title.value,
                                    podcast.imageToShow?.value ?: "",
                                    calculateTotalSize(nnListOfFiles)
                                )
                                )
                            }
                        }
                    }
                }
                viewStateContainer.updateViewState(DeleteMediaViewState.SectionList(sectionList))
            }
        }
    }

    private fun getLocalFilesGroupedByFeed(feedItems: List<FeedItem>): Map<FeedId?, List<File>> {
        return feedItems.groupBy({ it.feedId }, { it.localFile as File })
    }

    private fun calculateTotalSize(files: List<File>): String {
        var totalSize = 0L

        for (file in files) {
            if (file.exists()) {
                totalSize += file.length()
            }
        }

        val kb: Double = 1024.0
        val mb: Double = kb * 1024
        val gb: Double = mb * 1024

        val decimalFormat = DecimalFormat("#.##")

        return when {
            totalSize < kb -> "$totalSize Bytes"
            totalSize < mb -> "${decimalFormat.format(totalSize / kb)} KB"
            totalSize < gb -> "${decimalFormat.format(totalSize / mb)} MB"
            else -> "${decimalFormat.format(totalSize / gb)} GB"
        }
    }


}

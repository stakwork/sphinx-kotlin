package chat.sphinx.example.delete_media_detail.ui

import android.app.Application
import android.content.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.example.delete_media_detail.model.EpisodeToDelete
import chat.sphinx.example.delete_media_detail.navigation.DeleteMediaDetailNavigator
import chat.sphinx.example.delete_media_detail.viewstate.DeleteMediaDetailViewState
import chat.sphinx.example.delete_media_detail.viewstate.DeleteNotificationViewState
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.toFeedId
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
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        DeleteDetailNotifySideEffect,
        DeleteMediaDetailViewState
        >(dispatchers, DeleteMediaDetailViewState.Idle)
{

    private val args: DeleteMediaDetailFragmentArgs by savedStateHandle.navArgs()

    init {
        viewModelScope.launch(mainImmediate) {
            feedRepository.getDownloadedFeedItemsByFeedId(FeedId(args.argFeedId)).collect { feedItemList ->
                val feedName = feedRepository.getFeedById(FeedId(args.argFeedId)).firstOrNull()?.titleToShow ?: ""
                val episodeToDeleteList: List<EpisodeToDelete> = feedItemList.map { feedItem ->
                    EpisodeToDelete(
                        feedItem,
                        calculateFileSize(feedItem.localFile)
                    )
                }
                updateViewState(DeleteMediaDetailViewState.EpisodeList(feedName, episodeToDeleteList))
            }

        }
    }

    private fun calculateFileSize(file: File?): String {
        if (file == null) return ""

        val totalSize = if (file.exists()) file.length() else 0L

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

    val deleteNotificationViewStateContainer: ViewStateContainer<DeleteNotificationViewState> by lazy {
        ViewStateContainer(DeleteNotificationViewState.Closed)
    }

    fun openDeleteItemPopUp() {
        deleteNotificationViewStateContainer.updateViewState(DeleteNotificationViewState.Open)
    }
    fun closeDeleteItemPopUp() {
        deleteNotificationViewStateContainer.updateViewState(DeleteNotificationViewState.Closed)
    }

}

package chat.sphinx.episode_detail.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_connectivity_helper.ConnectivityHelper
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.episode_detail.R
import chat.sphinx.episode_detail.model.EpisodeDetail
import chat.sphinx.episode_detail.navigation.EpisodeDetailNavigator
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.toFeedId
import chat.sphinx.wrapper_feed.DownloadableFeedItem
import chat.sphinx.wrapper_feed.FeedItem
import chat.sphinx.wrapper_feed.toFeedTitle
import chat.sphinx.wrapper_podcast.PodcastEpisode
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.*
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject


@HiltViewModel
internal class EpisodeDetailViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    private val app: Application,
    private val feedRepository: FeedRepository,
    private val repositoryMedia: RepositoryMedia,
    val navigator: EpisodeDetailNavigator,
): SideEffectViewModel<
        Context,
        EpisodeDetailSideEffect,
        EpisodeDetailViewState,
        >(dispatchers, EpisodeDetailViewState.Idle)
{
    private val args: EpisodeDetailFragmentArgs by savedStateHandle.navArgs()

    companion object {
        const val YOUTUBE_TYPE = "Youtube"
        const val PODCAST_TYPE = "Podcast"
    }

    private val _feedItemIdStateFlow: MutableStateFlow<FeedItem?> by lazy {
        MutableStateFlow(null)
    }

    private val feedItemIdStateFlow: StateFlow<FeedItem?>
        get() = _feedItemIdStateFlow.asStateFlow()

    private val _playedMarkStateFlow: MutableStateFlow<Boolean> by lazy {
        MutableStateFlow(false)
    }

    private val playedMarkStateFlow: StateFlow<Boolean>
        get() = _playedMarkStateFlow.asStateFlow()

    private val _episodeDetailStateFlow: MutableStateFlow<EpisodeDetail> by lazy {
        MutableStateFlow(
            EpisodeDetail(
                args.argFeedId?.toFeedId(),
                args.argHeader,
                args.argImage,
                args.argEpisodeTypeImage,
                args.argEpisodeTypeText,
                args.argEpisodeDate,
                args.argEpisodeDuration,
                args.argDownloaded,
                isFeedItemDownloadInProgress(args.argFeedId?.toFeedId()),
                args.argLink,
                _playedMarkStateFlow.value
            )
        )
    }

    private val episodeDetailStateFlow: StateFlow<EpisodeDetail>
        get() = _episodeDetailStateFlow.asStateFlow()


    init {
        getFeedItem(args.argFeedId?.toFeedId())
        getPlayedMark()
        updateViewState(EpisodeDetailViewState.ShowEpisode(episodeDetailStateFlow.value))
    }

    private fun getFeedItem(feedId: FeedId?) {
        viewModelScope.launch(mainImmediate) {
            feedId?.let {
                feedRepository.getFeedItemById(it).firstOrNull()?.let { feedItem ->
                    _feedItemIdStateFlow.value = feedItem
                }
            }
        }
    }

    private fun getPlayedMark() {
        viewModelScope.launch(mainImmediate) {
            args.argFeedId?.toFeedId()?.let { itemId ->
                feedRepository.getPlayedMark(itemId).collect { played ->
                    _episodeDetailStateFlow.value = _episodeDetailStateFlow.value.copy(
                        played = played ?: false
                    )
                    updateViewState(EpisodeDetailViewState.ShowEpisode(episodeDetailStateFlow.value))
                }
            }
        }
    }

    fun updatePlayedMark(played: Boolean) {
        args.argFeedId?.toFeedId()?.let {itemId ->
            feedRepository.updatePlayedMark(itemId, played)
        }
        getPlayedMark()
    }

    fun downloadMedia() {
        feedItemIdStateFlow.value?.let { feedItem ->
            viewModelScope.launch(mainImmediate) {
                repositoryMedia.downloadMediaIfApplicable(feedItem) { _ ->
                    _episodeDetailStateFlow.value = _episodeDetailStateFlow.value.copy(downloaded = true, isDownloadInProgress = false)
                    updateViewState(EpisodeDetailViewState.ShowEpisode(episodeDetailStateFlow.value))

                }
                val isFeedItemDownloadInProgress = repositoryMedia.inProgressDownloadIds().contains(episodeDetailStateFlow.value.feedId)

                _episodeDetailStateFlow.value = _episodeDetailStateFlow.value.copy(
                        isDownloadInProgress = isFeedItemDownloadInProgress
                    )

                updateViewState(EpisodeDetailViewState.ShowEpisode(episodeDetailStateFlow.value))
            }
        }
    }

    fun deleteDownloadedMedia() {
        feedItemIdStateFlow.value?.let { feedItem ->
            viewModelScope.launch(mainImmediate) {
                if (repositoryMedia.deleteDownloadedMediaIfApplicable(feedItem)) {
                    _episodeDetailStateFlow.value =
                        _episodeDetailStateFlow.value.copy(downloaded = false)
                }
                updateViewState(EpisodeDetailViewState.ShowEpisode(episodeDetailStateFlow.value))
            }
        }
    }

    fun copyCodeToClipboard() {
        (app.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.let { manager ->
            val clipData = ClipData.newPlainText("text", args.argLink)
            manager.setPrimaryClip(clipData)

            viewModelScope.launch(mainImmediate) {
                submitSideEffect(
                    EpisodeDetailSideEffect(app.getString(R.string.episode_detail_clipboard))
                )
            }
        }
    }

    fun shareCodeThroughTextIntent(): Intent {
        val sharingIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, args.argLink)
        }

        return Intent.createChooser(
            sharingIntent,
            app.getString(R.string.episode_detail_share_link)
        )
    }


    fun popBackStack(){
        viewModelScope.launch(mainImmediate) {
            if(args.argEpisodeTypeText == YOUTUBE_TYPE){
                navigator.closeDetailScreen()
            }
            else {
                navigator.popBackStack()
            }
        }
    }

    private fun isFeedItemDownloadInProgress(feedItemId: FeedId?): Boolean {
        return repositoryMedia.inProgressDownloadIds().contains(feedItemId)
    }

}

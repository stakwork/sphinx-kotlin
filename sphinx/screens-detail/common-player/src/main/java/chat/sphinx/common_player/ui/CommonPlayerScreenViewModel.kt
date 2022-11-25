package chat.sphinx.common_player.ui

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.common_player.navigation.CommonPlayerNavigator
import chat.sphinx.common_player.viewstate.BoostAnimationViewState
import chat.sphinx.common_player.viewstate.CommonPlayerScreenViewState
import chat.sphinx.common_player.viewstate.EpisodePlayerViewState
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_common.ItemId
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.toSat
import chat.sphinx.wrapper_common.toItemId
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_feed.FeedRecommendation
import chat.sphinx.wrapper_feed.toFeedRecommendationOrNull
import chat.sphinx.wrapper_podcast.PodcastEpisode
import com.squareup.moshi.Moshi
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CommonPlayerScreenViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    private val navigator: CommonPlayerNavigator,
    private val contactRepository: ContactRepository,
    private val moshi: Moshi,
    private val mediaPlayerServiceController: MediaPlayerServiceController,
    savedStateHandle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        CommonPlayerScreenSideEffect,
        CommonPlayerScreenViewState,
        >(dispatchers, CommonPlayerScreenViewState.Idle), MediaPlayerServiceController.MediaServiceListener
{

    private val args: CommonPlayerScreenFragmentArgs by savedStateHandle.navArgs()

    val boostAnimationViewStateContainer: ViewStateContainer<BoostAnimationViewState> by lazy {
        ViewStateContainer(BoostAnimationViewState.Idle)
    }

    val episodePlayerViewStateContainer: ViewStateContainer<EpisodePlayerViewState> by lazy {
        ViewStateContainer(EpisodePlayerViewState.Idle)
    }

    init {
        mediaPlayerServiceController.addListener(this)

        loadRecommendations()

        viewModelScope.launch(mainImmediate) {
            val owner = getOwner()

            boostAnimationViewStateContainer.updateViewState(
                BoostAnimationViewState.BoosAnimationInfo(
                    owner.photoUrl,
                    owner.tipAmount
                )
            )
        }
    }

    private fun loadRecommendations() {
        var feedRecommendationList: MutableList<FeedRecommendation> = mutableListOf()
        var selectedRecommendation: FeedRecommendation? = null

        for (r in args.argRecommendations) {
            r.toFeedRecommendationOrNull(moshi)?.let { feedRecommendation ->
                feedRecommendationList.add(feedRecommendation)

                if (feedRecommendation.id == args.argRecommendationId) {
                    selectedRecommendation = feedRecommendation
                }
            }
        }

        selectedRecommendation?.let {
            itemSelected(it, feedRecommendationList)
        } ?: run {
            viewModelScope.launch(mainImmediate) {
                submitSideEffect(
                    CommonPlayerScreenSideEffect.Notify.ErrorLoadingRecommendations
                )

                navigator.closeDetailScreen()
            }
        }
    }

    fun itemSelected(
        feedRecommendation: FeedRecommendation,
        recommendations: List<FeedRecommendation>? = null
    ) {
        var feedRecommendationList: MutableList<FeedRecommendation> = mutableListOf()

        recommendations?.let {
            feedRecommendationList.addAll(it)
        }

        (currentViewState as? CommonPlayerScreenViewState.FeedRecommendations)?.let {
            stopPlayingEpisode(it.selectedItem)

            if (feedRecommendationList.isEmpty()) {
                feedRecommendationList.addAll(it.recommendations)
            }
        }

        if (feedRecommendation.isPodcast) {
            updateViewState(
                CommonPlayerScreenViewState.FeedRecommendations.PodcastSelected(
                    feedRecommendationList,
                    feedRecommendation
                )
            )
        } else if (feedRecommendation.isYouTubeVideo) {
            updateViewState(
                CommonPlayerScreenViewState.FeedRecommendations.YouTubeVideoSelected(
                    feedRecommendationList,
                    feedRecommendation
                )
            )
        } else {
            CommonPlayerScreenViewState.Idle
        }
    }

    private fun stopPlayingEpisode(feedRecommendation: FeedRecommendation) {
        viewModelScope.launch(mainImmediate) {
            feedRecommendation.resetPlayerData()

            if (feedRecommendation.isPodcast) {
                mediaPlayerServiceController.submitAction(
                    UserAction.ServiceAction.Pause(
                        ChatId(ChatId.NULL_CHAT_ID.toLong()),
                        feedRecommendation.id
                    )
                )
            }
        }
    }

    private suspend fun getOwner(): Contact {
        return contactRepository.accountOwner.value.let { contact ->
            if (contact != null) {
                contact
            } else {
                var resolvedOwner: Contact? = null
                try {
                    contactRepository.accountOwner.collect { ownerContact ->
                        if (ownerContact != null) {
                            resolvedOwner = ownerContact
                            throw Exception()
                        }
                    }
                } catch (e: Exception) {
                }
                delay(25L)

                resolvedOwner!!
            }
        }
    }

    override fun mediaServiceState(serviceState: MediaPlayerServiceState) {
        if (serviceState is MediaPlayerServiceState.ServiceActive.MediaState) {
            if (serviceState.chatId.value.toInt() != ChatId.NULL_CHAT_ID) {
                return
            }
        }

        (currentViewState as? CommonPlayerScreenViewState.FeedRecommendations)?.let { viewState ->

            viewModelScope.launch(mainImmediate) {
                @Exhaustive
                when (serviceState) {
                    is MediaPlayerServiceState.ServiceActive.MediaState.Playing -> {

                        viewState.recommendations.first {
                            it.id == serviceState.episodeId
                        }?.let { feedRecommendation ->
                            feedRecommendation.playingItemUpdate(
                                serviceState.currentTime,
                                serviceState.episodeDuration.toLong()
                            )

                            episodePlayerViewStateContainer.updateViewState(
                                EpisodePlayerViewState.MediaStateUpdate(
                                    feedRecommendation,
                                    serviceState
                                )
                            )

                            updateViewState(
                                CommonPlayerScreenViewState.FeedRecommendations.PodcastSelected(
                                    viewState.recommendations,
                                    feedRecommendation
                                )
                            )
                        }
                    }
                    is MediaPlayerServiceState.ServiceActive.MediaState.Paused -> {
                        viewState.recommendations.first {
                            it.id == serviceState.episodeId
                        }?.let { feedRecommendation ->
                            feedRecommendation.pauseItemUpdate()

                            episodePlayerViewStateContainer.updateViewState(
                                EpisodePlayerViewState.MediaStateUpdate(
                                    feedRecommendation,
                                    serviceState
                                )
                            )
                        }
                    }
                    is MediaPlayerServiceState.ServiceActive.MediaState.Ended -> {
                        viewState.recommendations.first {
                            it.id == serviceState.episodeId
                        }?.let { feedRecommendation ->
                            feedRecommendation.endEpisodeUpdate()

                            episodePlayerViewStateContainer.updateViewState(
                                EpisodePlayerViewState.MediaStateUpdate(
                                    feedRecommendation,
                                    serviceState
                                )
                            )
                        }
                    }
                    is MediaPlayerServiceState.ServiceActive.ServiceConnected -> {}
                    is MediaPlayerServiceState.ServiceActive.ServiceLoading -> {
                        episodePlayerViewStateContainer.updateViewState(EpisodePlayerViewState.ServiceLoading)
                    }
                    is MediaPlayerServiceState.ServiceInactive -> {
                        episodePlayerViewStateContainer.updateViewState(EpisodePlayerViewState.ServiceInactive)
                    }
                }
            }
        }
    }

    fun playEpisode(
        feedRecommendation: FeedRecommendation,
        startTime: Int,
        speed: Double
    ) {
        viewModelScope.launch(mainImmediate) {
            episodePlayerViewStateContainer.updateViewState(EpisodePlayerViewState.LoadingEpisode(feedRecommendation))

            delay(50L)

            mediaPlayerServiceController.submitAction(
                UserAction.ServiceAction.Play(
                    ChatId(ChatId.NULL_CHAT_ID.toLong()),
                    "feed-recommendation-podcast",
                    feedRecommendation.id,
                    feedRecommendation.link,
                    Sat(0),
                    speed,
                    startTime,
                )
            )

            feedRecommendation.playingItemUpdate(
                startTime,
                0
            )

            episodePlayerViewStateContainer.updateViewState(
                EpisodePlayerViewState.EpisodePlayed(
                    feedRecommendation
                )
            )
        }
    }

    fun pauseEpisode(feedRecommendation: FeedRecommendation) {
        viewModelScope.launch(mainImmediate) {
            feedRecommendation.pauseItemUpdate()

            mediaPlayerServiceController.submitAction(
                UserAction.ServiceAction.Pause(
                    ChatId(ChatId.NULL_CHAT_ID.toLong()),
                    feedRecommendation.id
                )
            )
        }
    }

    fun seekTo(
        time: Int,
        speed: Double
    ) {
        viewModelScope.launch(mainImmediate) {
            (currentViewState as? CommonPlayerScreenViewState.FeedRecommendations.PodcastSelected)?.let { viewState ->
                mediaPlayerServiceController.submitAction(
                    UserAction.ServiceAction.Seek(
                        ChatId(ChatId.NULL_CHAT_ID.toLong()),
                        ChatMetaData(
                            FeedId(viewState.selectedItem.id),
                            ItemId(ChatId.NULL_CHAT_ID.toLong()),
                            Sat(0),
                            time / 1000,
                            speed,
                        )
                    )
                )
            }
        }
    }

    fun adjustSpeed(speed: Double) {
        viewModelScope.launch(mainImmediate) {
            (currentViewState as? CommonPlayerScreenViewState.FeedRecommendations.PodcastSelected)?.let { viewState ->
                mediaPlayerServiceController.submitAction(
                    UserAction.AdjustSpeed(
                        ChatId(ChatId.NULL_CHAT_ID.toLong()),
                        ChatMetaData(
                            FeedId(viewState.selectedItem.id),
                            ItemId(ChatId.NULL_CHAT_ID.toLong()),
                            Sat(0),
                            0,
                            speed,
                        )
                    )
                )
            }
        }
    }

    fun retrieveItemDuration(episodeUrl: String, localFile: File?): Long {
        localFile?.let {
            return Uri.fromFile(it).getMediaDuration(true)
        } ?: run {
            return Uri.parse(episodeUrl).getMediaDuration(false)
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayerServiceController.removeListener(this)
    }
}

fun Uri.getMediaDuration(
    isLocalFile: Boolean
): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        if (Build.VERSION.SDK_INT >= 14 && !isLocalFile) {
            retriever.setDataSource(this.toString(), HashMap<String, String>())
        } else {
            retriever.setDataSource(this.toString())
        }
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()
        duration?.toLongOrNull() ?: 0
    } catch (exception: Exception) {
        0
    }
}
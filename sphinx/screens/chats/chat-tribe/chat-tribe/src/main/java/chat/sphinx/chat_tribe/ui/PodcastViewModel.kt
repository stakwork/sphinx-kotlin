package chat.sphinx.chat_tribe.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.chat_tribe.R
import chat.sphinx.chat_tribe.model.TribePodcastData
import chat.sphinx.chat_tribe.navigation.TribeChatNavigator
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_network_query_chat.model.toPodcast
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.podcast_player.objects.toParcelablePodcast
import chat.sphinx.podcast_player.ui.getMediaDuration
import chat.sphinx.wrapper_chat.isTribeOwnedByAccount
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_contact.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewState
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
internal class PodcastViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    handle: SavedStateHandle,
    private val app: Application,
    private val accountOwner: StateFlow<Contact?>,
    private val navigator: TribeChatNavigator,
    private val chatRepository: ChatRepository,
    private val messageRepository: MessageRepository,
    private val networkQueryChat: NetworkQueryChat,
    private val mediaPlayerServiceController: MediaPlayerServiceController,
) : BaseViewModel<PodcastViewState>(dispatchers, PodcastViewState.NoPodcast),
    MediaPlayerServiceController.MediaServiceListener
{
    private val args: ChatTribeFragmentArgs by handle.navArgs()

    val boostAnimationViewStateContainer: ViewStateContainer<BoostAnimationViewState> by lazy {
        ViewStateContainer(BoostAnimationViewState.Idle)
    }

    private inner class PodcastContributionsViewStateContainer: ViewStateContainer<PodcastContributionsViewState>(PodcastContributionsViewState.None) {
        override val viewStateFlow: StateFlow<PodcastContributionsViewState> =
            flow {
                collectViewState { podcastViewState ->
                    @Exhaustive
                    when (podcastViewState) {
                        is PodcastViewState.NoPodcast -> {
                            emit(PodcastContributionsViewState.None)
                        }
                        is PodcastViewState.Available -> {
                            chatRepository.getChatById(args.chatId).firstOrNull()?.let { chat ->
                                val owner = getOwner()

                                messageRepository.getPaymentsTotalFor(podcastViewState.podcast.id).collect { paymentsTotal ->
                                    if (paymentsTotal != null) {
                                        val isMyTribe = chat.isTribeOwnedByAccount(owner.nodePubKey)
                                        val label = app.getString(
                                            if (isMyTribe) {
                                                R.string.chat_tribe_earned
                                            } else {
                                                R.string.chat_tribe_contributed
                                            }
                                        )

                                        emit(PodcastContributionsViewState.Contributions(
                                            label + " ${paymentsTotal.asFormattedString(appendUnit = true)}"
                                        ))
                                    }
                                }
                            }
                        }
                    }
                }
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(2_000),
                PodcastContributionsViewState.None
            )
    }

    val contributionsViewStateContainer: ViewStateContainer<PodcastContributionsViewState> by lazy {
        PodcastContributionsViewStateContainer()
    }

    init {
        viewModelScope.launch(mainImmediate) {
            val owner = getOwner()

            boostAnimationViewStateContainer.updateViewState(
                BoostAnimationViewState.BoosAnimationInfo(
                    owner.photoUrl,
                    owner.tipAmount,
                )
            )
        }
    }

    override fun mediaServiceState(serviceState: MediaPlayerServiceState) {
        if (serviceState is MediaPlayerServiceState.ServiceActive.MediaState) {
            if (serviceState.chatId != args.chatId) {
                return
            }
        }

        val vs = currentViewState
        if (vs !is PodcastViewState.Available) {
            return
        }

        @Exhaustive
        when (serviceState) {
            is MediaPlayerServiceState.ServiceActive.MediaState.Playing -> {
                vs.podcast.playingEpisodeUpdate(
                    serviceState.episodeId,
                    serviceState.currentTime,
                    serviceState.episodeDuration.toLong()
                )

                updateViewState(
                    vs.adjustState(
                        showLoading = false,
                        showPlayButton = false,
                        title = vs.podcast.getCurrentEpisode().title,
                        playingProgress = vs.podcast.getPlayingProgress(::retrieveEpisodeDuration),
                    )
                )
            }
            is MediaPlayerServiceState.ServiceActive.MediaState.Paused -> {
                vs.podcast.pauseEpisodeUpdate()

                updateViewState(
                    vs.adjustState(
                        showLoading = false,
                        showPlayButton = true,
                        title = vs.podcast.getCurrentEpisode().title,
                        playingProgress = vs.podcast.getPlayingProgress(::retrieveEpisodeDuration),
                    )
                )
            }
            is MediaPlayerServiceState.ServiceActive.MediaState.Ended -> {
                vs.podcast.endEpisodeUpdate(
                    serviceState.episodeId,
                    ::retrieveEpisodeDuration
                )

                updateViewState(
                    vs.adjustState(
                        showLoading = false,
                        showPlayButton = true,
                        title = vs.podcast.getCurrentEpisode().title,
                        playingProgress = vs.podcast.getPlayingProgress(::retrieveEpisodeDuration),
                    )
                )
            }
            is MediaPlayerServiceState.ServiceActive.ServiceConnected -> {
                viewModelScope.launch(mainImmediate) {
                    mediaPlayerServiceController.submitAction(
                        UserAction.SetPaymentsDestinations(
                            args.chatId,
                            vs.podcast.value.destinations,
                        )
                    )
                }
            }
            is MediaPlayerServiceState.ServiceActive.ServiceLoading -> {
                updateViewState(vs.adjustState(showLoading = true))
            }
            is MediaPlayerServiceState.ServiceInactive -> {
                vs.podcast.pauseEpisodeUpdate()

                updateViewState(
                    vs.adjustState(
                        showLoading = false,
                        showPlayButton = true,
                        title = vs.podcast.getCurrentEpisode().title,
                        playingProgress = vs.podcast.getPlayingProgress(::retrieveEpisodeDuration)
                    )
                )
            }
        }
    }

    override fun onCleared() {
        mediaPlayerServiceController.removeListener(this)
    }

    @Volatile
    private var initialized: Boolean = false
    fun init(data: TribePodcastData.Result) {
        if (initialized) {
            return
        } else {
            initialized = true
        }

        @Exhaustive
        when (data) {
            is TribePodcastData.Result.NoPodcast -> { /* no-op */}
            is TribePodcastData.Result.TribeData -> {
                viewModelScope.launch(mainImmediate) {
                    networkQueryChat.getPodcastFeed(data.host, data.feedUrl.value).collect { response ->
                        @Exhaustive
                        when (response) {
                            is LoadResponse.Loading -> {}
                            is Response.Error -> {}
                            is Response.Success -> {
                                val podcast = response.value.toPodcast()

                                if (data.metaData != null) {
                                    podcast.setMetaData(data.metaData)
                                }

                                PodcastViewState.Available(
                                    showLoading = false,
                                    showPlayButton = !podcast.isPlaying,
                                    title = podcast.getCurrentEpisode().title,
                                    playingProgress = withContext(io) { podcast.getPlayingProgress(::retrieveEpisodeDuration) },
                                    clickPlayPause = OnClickCallback {

                                        val vs = currentViewState

                                        if (vs !is PodcastViewState.Available) {
                                            return@OnClickCallback
                                        }

                                        val episode = vs.podcast.getCurrentEpisode()

                                        viewModelScope.launch {
                                            if (episode.playing) {

                                                vs.podcast.didPausePlayingEpisode(episode)

                                                mediaPlayerServiceController.submitAction(
                                                    UserAction.ServiceAction.Pause(
                                                        args.chatId,
                                                        episode.id
                                                    )
                                                )
                                            } else {
                                                withContext(io) {
                                                    vs.podcast.didStartPlayingEpisode(
                                                        episode,
                                                        vs.podcast.currentTime,
                                                        ::retrieveEpisodeDuration,
                                                    )
                                                }

                                                mediaPlayerServiceController.submitAction(
                                                    UserAction.ServiceAction.Play(
                                                        args.chatId,
                                                        vs.podcast.id,
                                                        episode.id,
                                                        episode.enclosureUrl,
                                                        Sat(vs.podcast.satsPerMinute),
                                                        vs.podcast.speed,
                                                        vs.podcast.currentTime,
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    clickBoost = OnClickCallback {
                                        viewModelScope.launch(mainImmediate) {

                                            val owner: Contact = getOwner()

                                            owner.tipAmount?.let { tip ->
                                                if (tip.value > 0) {

                                                    val vs = currentViewState

                                                    if (vs is PodcastViewState.Available) {
                                                        val metaData = vs.podcast.getMetaData(tip)

                                                        messageRepository.sendPodcastBoost(
                                                            args.chatId,
                                                            vs.podcast
                                                        )

                                                        mediaPlayerServiceController.submitAction(
                                                            UserAction.SendBoost(
                                                                args.chatId,
                                                                vs.podcast.id,
                                                                metaData,
                                                                vs.podcast.value.destinations,
                                                            )
                                                        )
                                                    }
                                                }
                                            }

                                        }
                                    },
                                    clickFastForward = OnClickCallback {
                                        val vs = currentViewState

                                        if (vs !is PodcastViewState.Available) {
                                            return@OnClickCallback
                                        }

                                        viewModelScope.launch(mainImmediate) {
                                            vs.podcast.didSeekTo(vs.podcast.currentTime + 30_000)

                                            mediaPlayerServiceController.submitAction(
                                                UserAction.ServiceAction.Seek(
                                                    args.chatId,
                                                    vs.podcast.getMetaData()
                                                )
                                            )

                                            if (!vs.podcast.isPlaying) {
                                                updateViewState(
                                                    vs.adjustState(
                                                        showPlayButton = true,
                                                        playingProgress = vs.podcast.getPlayingProgress(::retrieveEpisodeDuration),
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    clickTitle = OnClickCallback {
                                        val vs = currentViewState

                                        if (vs !is PodcastViewState.Available) {
                                            return@OnClickCallback
                                        }

                                        viewModelScope.launch(mainImmediate) {
                                            navigator.toPodcastPlayerScreen(
                                                args.chatId,
                                                vs.podcast.toParcelablePodcast(),
                                            )
                                        }
                                    },
                                    podcast

                                ).let { initialViewState ->

                                    // set our initial view state prior to registering listener
                                    // such that in the event this chat's podcast is playing,
                                    // the initial dispatch will be registered and update the UI
                                    // appropriately
                                    updateViewState(initialViewState)
                                    mediaPlayerServiceController.addListener(this@PodcastViewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun getOwner(): Contact {
        return accountOwner.value.let { contact ->
            if (contact != null) {
                contact
            } else {
                var resolvedOwner: Contact? = null
                try {
                    accountOwner.collect { ownerContact ->
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

    private fun retrieveEpisodeDuration(episodeUrl: String): Long {
        val uri = Uri.parse(episodeUrl)
        return uri.getMediaDuration()
    }
}

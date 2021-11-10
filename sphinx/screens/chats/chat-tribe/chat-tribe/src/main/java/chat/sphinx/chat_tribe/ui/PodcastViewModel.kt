package chat.sphinx.chat_tribe.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.chat_tribe.R
import chat.sphinx.chat_tribe.model.TribePodcastData
import chat.sphinx.chat_tribe.navigation.TribeChatNavigator
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_repository_message.MessageRepository
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.podcast_player.ui.getMediaDuration
import chat.sphinx.wrapper_chat.isTribeOwnedByAccount
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_podcast.Podcast
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
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
    private val repositoryMedia: RepositoryMedia,
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
                    when (podcastViewState) {
                        is PodcastViewState.NoPodcast -> {
                            emit(PodcastContributionsViewState.None)
                        }
                        is PodcastViewState.PodcastVS.Available -> {
                            chatRepository.getChatById(args.chatId).firstOrNull()?.let { chat ->
                                val owner = getOwner()

                                podcastViewState.podcast.id.value.toLongOrNull()?.let { podcastId ->
                                    messageRepository.getPaymentsTotalFor(podcastId).collect { paymentsTotal ->
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
                        else -> {}
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

        mediaPlayerServiceController.addListener(this@PodcastViewModel)
    }

    private var currentServiceState: MediaPlayerServiceState = MediaPlayerServiceState.ServiceInactive
    override fun mediaServiceState(serviceState: MediaPlayerServiceState) {
        if (serviceState is MediaPlayerServiceState.ServiceActive.MediaState) {
            if (serviceState.chatId != args.chatId) {
                return
            }
        }

        currentServiceState = serviceState

        val vs = currentViewState
        if (vs !is PodcastViewState.PodcastVS) {
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

                vs.adjustState(
                    showLoading = false,
                    showPlayButton = false,
                    title = vs.podcast.getCurrentEpisode().title.value,
                    playingProgress = vs.podcast.getPlayingProgress(::retrieveEpisodeDuration),
                )?.let {
                    updateViewState(it)
                }
            }
            is MediaPlayerServiceState.ServiceActive.MediaState.Paused -> {
                vs.podcast.pauseEpisodeUpdate()

                vs.adjustState(
                    showLoading = false,
                    showPlayButton = true,
                    title = vs.podcast.getCurrentEpisode().title.value,
                    playingProgress = vs.podcast.getPlayingProgress(::retrieveEpisodeDuration),
                )?.let {
                    updateViewState(it)
                }
            }
            is MediaPlayerServiceState.ServiceActive.MediaState.Ended -> {
                vs.podcast.endEpisodeUpdate(
                    serviceState.episodeId,
                    ::retrieveEpisodeDuration
                )

                vs.adjustState(
                    showLoading = false,
                    showPlayButton = true,
                    title = vs.podcast.getCurrentEpisode().title.value,
                    playingProgress = vs.podcast.getPlayingProgress(::retrieveEpisodeDuration),
                )?.let {
                    updateViewState(it)
                }
            }
            is MediaPlayerServiceState.ServiceActive.ServiceConnected -> {
                viewModelScope.launch(mainImmediate) {
                    mediaPlayerServiceController.submitAction(
                        UserAction.SetPaymentsDestinations(
                            chatId = args.chatId,
                            destinations = vs.podcast.destinations,
                        )
                    )
                }
            }
            is MediaPlayerServiceState.ServiceActive.ServiceLoading -> {
                vs.adjustState(showLoading = true)?.let {
                    updateViewState(it)
                }
            }
            is MediaPlayerServiceState.ServiceInactive -> {
                vs.podcast.pauseEpisodeUpdate()

                vs.adjustState(
                    showLoading = false,
                    showPlayButton = true,
                    title = vs.podcast.getCurrentEpisode().title.value,
                    playingProgress = vs.podcast.getPlayingProgress(::retrieveEpisodeDuration)
                )?.let {
                    updateViewState(it)
                }
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
                    chatRepository.updatePodcastFeed(
                        args.chatId,
                        data.host,
                        data.feedUrl
                    )
                }

                viewModelScope.launch(mainImmediate) {
                    delay(500L)
                    chatRepository.getPodcastByChatId(args.chatId).collect { podcast ->
                        podcast?.let { nnPodcast ->
                            podcastLoaded(
                                nnPodcast,
                                data
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun podcastLoaded(
        podcast: Podcast,
        tribeData: TribePodcastData.Result.TribeData
    ) {

        if (tribeData.metaData != null) {
            podcast.setMetaData(tribeData.metaData)
        }

        val clickPlayPause = OnClickCallback {

            val vs = currentViewState

            if (vs !is PodcastViewState.PodcastVS) {
                return@OnClickCallback
            }

            val episode = vs.podcast.getCurrentEpisode()

            viewModelScope.launch {
                episode.id.value.toLongOrNull()?.let { episodeId ->
                    if (episode.playing) {
                        vs.podcast.didPausePlayingEpisode(episode)

                        mediaPlayerServiceController.submitAction(
                            UserAction.ServiceAction.Pause(
                                args.chatId,
                                episodeId
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

                        vs.podcast.id.value.toLongOrNull()
                            ?.let { podcastId ->
                                mediaPlayerServiceController.submitAction(
                                    UserAction.ServiceAction.Play(
                                        args.chatId,
                                        podcastId,
                                        episodeId,
                                        episode.enclosureUrl.value,
                                        Sat(vs.podcast.satsPerMinute),
                                        vs.podcast.speed,
                                        vs.podcast.currentTime,
                                    )
                                )
                            }
                    }
                }
            }
        }

        val clickBoost = OnClickCallback {
            viewModelScope.launch(mainImmediate) {

                val owner: Contact = getOwner()

                owner.tipAmount?.let { tip ->
                    if (tip.value > 0) {

                        val vs = currentViewState

                        if (vs is PodcastViewState.PodcastVS) {
                            val metaData = vs.podcast.getMetaData(tip)

                            messageRepository.sendPodcastBoost(
                                args.chatId,
                                vs.podcast
                            )

                            vs.podcast.id.value.toLongOrNull()?.let { podcastId ->
                                mediaPlayerServiceController.submitAction(
                                    UserAction.SendBoost(
                                        args.chatId,
                                        podcastId,
                                        metaData,
                                        vs.podcast.destinations ?: arrayListOf(),
                                    )
                                )
                            }
                        }
                    }
                }

            }
        }

        val clickFastForward = OnClickCallback {
            val vs = currentViewState

            if (vs !is PodcastViewState.PodcastVS) {
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
                    vs.adjustState(
                        showPlayButton = true,
                        playingProgress = vs.podcast.getPlayingProgress(::retrieveEpisodeDuration),
                    )?.let {
                        updateViewState(it)
                    }
                }
            }
        }

        val clickTitle = OnClickCallback {
            val vs = currentViewState

            if (vs !is PodcastViewState.PodcastVS) {
                return@OnClickCallback
            }

            repositoryMedia.updateChatMetaData(
                args.chatId,
                vs.podcast.getMetaData(),
                false
            )

            viewModelScope.launch(mainImmediate) {
                navigator.toPodcastPlayerScreen(
                    chatId = args.chatId,
                    currentEpisodeDuration = vs.podcast.episodeDuration ?: 0
                )
            }
        }

        val isPlaying = (currentServiceState is MediaPlayerServiceState.ServiceActive.MediaState.Playing)

        if (!isPlaying) {
            PodcastViewState.PodcastVS.Available(
                showLoading = true,
                showPlayButton = true,
                title = podcast.getCurrentEpisode().title.value,
                playingProgress = 0,
                clickPlayPause = clickPlayPause,
                clickBoost = clickBoost,
                clickFastForward = clickFastForward,
                clickTitle = clickTitle,
                podcast
            ).let { initialViewState ->
                updateViewState(initialViewState)
            }
        }

        val currentSS = currentServiceState

        val playingProgress = if (currentSS is MediaPlayerServiceState.ServiceActive.MediaState.Playing) {
            podcast.getPlayingProgress(currentSS.episodeDuration)
        } else {
            withContext(io) { podcast.getPlayingProgress(::retrieveEpisodeDuration) }
        }

        PodcastViewState.PodcastVS.Ready(
            showLoading = false,
            showPlayButton = !isPlaying,
            title = podcast.getCurrentEpisode().title.value,
            playingProgress = playingProgress,
            clickPlayPause = clickPlayPause,
            clickBoost = clickBoost,
            clickFastForward = clickFastForward,
            clickTitle = clickTitle,
            podcast
        ).let { initialViewState ->
            updateViewState(initialViewState)
        }
    }

    val satsPerMinuteStateFlow: StateFlow<Boolean> =
        flow {
            collectViewState { viewState ->
                if (viewState is PodcastViewState.PodcastVS.Available) {
                    chatRepository.getChatById(args.chatId).collect { chat ->
                        chat?.metaData?.let { nnMetaData ->
                            val vs = currentViewState
                            if (vs is PodcastViewState.PodcastVS) {
                                vs.podcast.satsPerMinute = nnMetaData.satsPerMinute.value
                            }
                        }
                        emit(true)
                    }
                }
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(2_000),
            true
        )

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

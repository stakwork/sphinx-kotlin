package chat.sphinx.chat_tribe.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.exhaustive.Exhaustive
import chat.sphinx.chat_tribe.model.TribePodcastData
import chat.sphinx.concept_network_query_chat.NetworkQueryChat
import chat.sphinx.concept_network_query_chat.model.toPodcast
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class PodcastViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    handle: SavedStateHandle,
    private val networkQueryChat: NetworkQueryChat,
    private val mediaPlayerServiceController: MediaPlayerServiceController,
) : BaseViewModel<PodcastViewState2>(dispatchers, PodcastViewState2.NoPodcast),
    MediaPlayerServiceController.MediaServiceListener
{
    private val args: ChatTribeFragmentArgs by handle.navArgs()

    override fun mediaServiceState(serviceState: MediaPlayerServiceState) {
        if (serviceState is MediaPlayerServiceState.ServiceActive.MediaState) {
            if (serviceState.chatId != args.chatId) {
                return
            }
        }

        @Exhaustive
        when (serviceState) {
            is MediaPlayerServiceState.ServiceActive.MediaState.Ended -> {

            }
            is MediaPlayerServiceState.ServiceActive.MediaState.Paused -> {

            }
            is MediaPlayerServiceState.ServiceActive.MediaState.Playing -> {

            }
            is MediaPlayerServiceState.ServiceActive.ServiceConnected -> {

            }
            is MediaPlayerServiceState.ServiceActive.ServiceLoading -> {

            }
            is MediaPlayerServiceState.ServiceInactive -> {

            }
        }
    }

    init {
        mediaPlayerServiceController.addListener(this)
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

                                updateViewState(
                                    PodcastViewState2.Available(
                                        showLoading = false,
                                        showPlayButton = podcast.isPlaying,
                                        title = podcast.title,
                                        durationProgress = 0, // TODO: Implement
                                        clickPlayPause = OnClickCallback {
                                            // TODO: Implement
                                        },
                                        clickBoost = OnClickCallback {
                                            // TODO: Implement
                                        },
                                        clickFastForward = OnClickCallback {
                                            // TODO: Implement
                                        },
                                        clickTitle = OnClickCallback {
                                            // TODO: Implement
                                        },
                                        podcast
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

package chat.sphinx.podcast_player.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.podcast_player.navigation.PodcastPlayerNavigator
import chat.sphinx.podcast_player.objects.Podcast
import chat.sphinx.podcast_player.objects.PodcastEpisode
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.dashboard.ChatId
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

internal inline val PodcastPlayerFragmentArgs.chatId: ChatId
    get() = ChatId(argChatId)

internal inline val PodcastPlayerFragmentArgs.podcast: Podcast
    get() = argPodcast

@HiltViewModel
internal class PodcastPlayerViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: PodcastPlayerNavigator,
    private val chatRepository: ChatRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<PodcastPlayerViewState>(dispatchers, PodcastPlayerViewState.Idle(listOf())) {

    private val args: PodcastPlayerFragmentArgs by savedStateHandle.navArgs()

    init {
        args.podcast?.let { podcast ->
            viewStateContainer.updateViewState(PodcastPlayerViewState.PodcastLoaded(podcast, podcast.episodes))
        }
    }

    private val chatSharedFlow: SharedFlow<Chat?> = flow {
        emitAll(chatRepository.getChatById(args.chatId))
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    fun playPauseEpisode(podcast: Podcast?, episode: PodcastEpisode) {
        viewModelScope.launch(mainImmediate) {
            chatSharedFlow.collect { chat ->
                chat?.let { chat ->
                    podcast?.let { podcast ->
                        if (episode.playing) {
                            updateMetaData(chat, episode, podcast)
                            //TODO Pause play action to Service
                        } else {
                            updateMetaData(chat, episode, podcast)

                            viewStateContainer.updateViewState(
                                PodcastPlayerViewState.EpisodePlayed(
                                    podcast,
                                    podcast.episodes
                                )
                            )
                            //TODO Send play action to Service
                        }
                    }
                }
            }
        }
    }

    private fun updateMetaData(chat: Chat, episode: PodcastEpisode, podcast: Podcast) {
        //Update chat MetaData (time: time, episodeId: episode.id, speed: podcast.speed, satsPerMinute: podcast.satsPerMinute)
        //Set ChatMetaData on Podcast
    }

}

package chat.sphinx.podcast_player.ui

import androidx.lifecycle.SavedStateHandle
import chat.sphinx.podcast_player.navigation.PodcastPlayerNavigator
import chat.sphinx.podcast_player.objects.PodcastEpisode
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject


@HiltViewModel
internal class PodcastPlayerViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: PodcastPlayerNavigator,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<PodcastPlayerViewState>(dispatchers, PodcastPlayerViewState.Idle(listOf())) {

    private val args: PodcastPlayerFragmentArgs by savedStateHandle.navArgs()

    init {
        args.argPodcast?.let { podcast ->
            viewStateContainer.updateViewState(PodcastPlayerViewState.PodcastObject(podcast, podcast.episodes))
        }
    }

    fun playPauseEpisode(episode: PodcastEpisode) {
        if (episode.playing) {
            //TODO Send pause action to Service
        } else {
            //TODO Send play action to Service
        }
    }

}

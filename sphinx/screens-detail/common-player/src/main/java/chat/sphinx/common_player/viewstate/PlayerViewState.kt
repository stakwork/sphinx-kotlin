package chat.sphinx.common_player.viewstate

import chat.sphinx.wrapper_podcast.PodcastEpisode
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class PlayerViewState: ViewState<PlayerViewState>() {

    object Idle: PlayerViewState()

    object PodcastEpisodeSelected : PlayerViewState()

    class YouTubeVideoSelected(
        val episode: PodcastEpisode
    ) : PlayerViewState()
}

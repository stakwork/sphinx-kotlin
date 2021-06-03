package chat.sphinx.podcast_player.ui

import chat.sphinx.podcast_player.navigation.TribeChatPodcastPlayerNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject


@HiltViewModel
internal class TribeChatPodcastPlayerViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: TribeChatPodcastPlayerNavigator
) : BaseViewModel<TribeChatPodcastPlayerViewState>(dispatchers, TribeChatPodcastPlayerViewState.Idle) {
}

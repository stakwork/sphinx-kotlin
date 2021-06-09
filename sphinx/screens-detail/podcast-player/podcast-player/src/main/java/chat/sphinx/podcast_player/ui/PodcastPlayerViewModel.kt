package chat.sphinx.podcast_player.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.podcast_player.navigation.PodcastPlayerNavigator
import chat.sphinx.wrapper_common.util.getInitials
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject


@HiltViewModel
internal class PodcastPlayerViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: PodcastPlayerNavigator,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<PodcastPlayerViewState>(dispatchers, PodcastPlayerViewState.Idle) {

    private val args: PodcastPlayerFragmentArgs by savedStateHandle.navArgs()

    val podcastSharedFlow: SharedFlow<PodcastPlayerViewState> = flow {
        args.argPodcast?.let { podcast ->
            emit(PodcastPlayerViewState.PodcastObject(podcast))
        }
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

}

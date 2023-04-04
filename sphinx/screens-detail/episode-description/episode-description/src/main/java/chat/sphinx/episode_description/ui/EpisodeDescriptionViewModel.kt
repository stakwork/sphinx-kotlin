package chat.sphinx.episode_description.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.episode_description.navigation.EpisodeDescriptionNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.*
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
internal class EpisodeDescriptionViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    private val app: Application,
    private val feedRepository: FeedRepository,
    private val repositoryMedia: RepositoryMedia,
    private val mediaPlayerServiceController: MediaPlayerServiceController,
    val navigator: EpisodeDescriptionNavigator,
): SideEffectViewModel<
        Context,
        EpisodeDescriptionSideEffect,
        EpisodeDescriptionViewState,
        >(dispatchers, EpisodeDescriptionViewState.Idle)
{
    private val args: EpisodeDescriptionFragmentArgs by savedStateHandle.navArgs()

}

package chat.sphinx.threads.ui

import android.app.Application
import android.content.*
import androidx.lifecycle.SavedStateHandle
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
import chat.sphinx.threads.navigation.ThreadNavigator
import chat.sphinx.threads.viewstate.ThreadViewState
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class ThreadViewModel @Inject constructor(
    private val app: Application,
    val navigator: ThreadNavigator,
    private val repositoryMedia: RepositoryMedia,
    private val feedRepository: FeedRepository,
    dispatchers: CoroutineDispatchers,
    handle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        ThreadSideEffect,
        ThreadViewState
        >(dispatchers, ThreadViewState.Idle)
{

}

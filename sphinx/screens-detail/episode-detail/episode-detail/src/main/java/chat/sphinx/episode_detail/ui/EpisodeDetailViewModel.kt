package chat.sphinx.episode_detail.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.episode_detail.model.EpisodeDetail
import chat.sphinx.episode_detail.navigation.EpisodeDetailNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
internal class EpisodeDetailViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    val navigator: EpisodeDetailNavigator,
): SideEffectViewModel<
        Context,
        EpisodeDetailSideEffect,
        EpisodeDetailViewState,
        >(dispatchers, EpisodeDetailViewState.Idle)
{
    private val args: EpisodeDetailFragmentArgs by savedStateHandle.navArgs()

    fun popBackStack(){
        viewModelScope.launch(mainImmediate) {
            if(args.argEpisodeTypeText == "Youtube"){
                navigator.closeDetailScreen()
            }
            else {
                navigator.popBackStack()
            }
        }
    }

    init {
        updateViewState(EpisodeDetailViewState.ShowEpisode(
            EpisodeDetail(
                args.argHeader,
                args.argImage,
                args.argEpisodeTypeImage,
                args.argEpisodeTypeText,
                args.argEpisodeDate,
                args.argEpisodeDuration
            )
        )
        )
    }

}

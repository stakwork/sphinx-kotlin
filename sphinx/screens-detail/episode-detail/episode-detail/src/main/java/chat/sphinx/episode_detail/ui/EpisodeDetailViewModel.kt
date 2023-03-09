package chat.sphinx.episode_detail.ui

import androidx.lifecycle.viewModelScope
import chat.sphinx.episode_detail.navigation.EpisodeDetailNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
internal class EpisodeDetailViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: EpisodeDetailNavigator,
): BaseViewModel<EpisodeDetailViewState>(
    dispatchers,
    EpisodeDetailViewState.Idle
)
{
    fun popBackStack(){
        viewModelScope.launch(mainImmediate) {
            navigator.popBackStack()
        }
    }

}

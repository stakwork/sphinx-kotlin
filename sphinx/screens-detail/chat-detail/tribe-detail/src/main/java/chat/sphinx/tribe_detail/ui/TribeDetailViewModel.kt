package chat.sphinx.tribe_detail.ui

import chat.sphinx.tribe_detail.navigation.TribeDetailNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class TribeDetailViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: TribeDetailNavigator,
): BaseViewModel<TribeDetailViewState>(dispatchers, TribeDetailViewState.Idle)
{
}

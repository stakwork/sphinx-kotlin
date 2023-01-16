package chat.sphinx.discover_tribes.ui

import chat.sphinx.discover_tribes.navigation.DiscoverTribesNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class DiscoverTribesViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: DiscoverTribesNavigator
): BaseViewModel<DiscoverTribesViewState>(dispatchers, DiscoverTribesViewState.Idle)
{

}
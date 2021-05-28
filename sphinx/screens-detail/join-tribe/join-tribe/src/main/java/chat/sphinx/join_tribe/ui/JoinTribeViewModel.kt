package chat.sphinx.join_tribe.ui

import chat.sphinx.join_tribe.navigation.JoinTribeNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class JoinTribeViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: JoinTribeNavigator
): BaseViewModel<JoinTribeViewState>(dispatchers, JoinTribeViewState.Idle)
{
}

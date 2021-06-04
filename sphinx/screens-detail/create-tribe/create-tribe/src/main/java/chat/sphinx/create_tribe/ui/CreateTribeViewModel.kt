package chat.sphinx.create_tribe.ui

import chat.sphinx.create_tribe.navigation.CreateTribeNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class CreateTribeViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: CreateTribeNavigator,
): BaseViewModel<CreateTribeViewState>(dispatchers, CreateTribeViewState.Idle)
{
}

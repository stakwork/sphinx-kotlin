package chat.sphinx.add_sats.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class AddSatsViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
): BaseViewModel<AddSatsViewState>(dispatchers, AddSatsViewState.Idle)
{
}

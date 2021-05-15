package chat.sphinx.transactions.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class TransactionsViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
): BaseViewModel<TransactionsViewState>(dispatchers, TransactionsViewState.Idle)
{
}

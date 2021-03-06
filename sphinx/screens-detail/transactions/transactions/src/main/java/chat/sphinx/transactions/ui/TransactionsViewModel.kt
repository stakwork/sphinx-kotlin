package chat.sphinx.transactions.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import javax.inject.Inject

@HiltViewModel
internal class TransactionsViewModel @Inject constructor(

): BaseViewModel<TransactionsViewState>(TransactionsViewState.Idle)
{
}

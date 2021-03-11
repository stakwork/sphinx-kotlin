package chat.sphinx.transactions.ui

import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.transactions.R
import chat.sphinx.transactions.databinding.FragmentTransactionsBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment

@AndroidEntryPoint
internal class TransactionsFragment: BaseFragment<
        TransactionsViewState,
        TransactionsViewModel,
        FragmentTransactionsBinding
        >(R.layout.fragment_transactions)
{
    override val viewModel: TransactionsViewModel by viewModels()
    override val binding: FragmentTransactionsBinding by viewBinding(FragmentTransactionsBinding::bind)

    override suspend fun onViewStateFlowCollect(viewState: TransactionsViewState) {
//        TODO("Not yet implemented")
    }
}

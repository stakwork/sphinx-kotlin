package chat.sphinx.transactions.ui

import android.os.Bundle
import android.view.View
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.includeTransactionsHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.transactions_header_name)
//            textViewDetailScreenClose.setOnClickListener {
            // TODO: Navigate
//            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: TransactionsViewState) {
//        TODO("Not yet implemented")
    }
}

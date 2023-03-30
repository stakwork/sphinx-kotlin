package chat.sphinx.transactions.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.screen_detail_fragment.BaseDetailFragment
import chat.sphinx.transactions.R
import chat.sphinx.transactions.databinding.FragmentTransactionsBinding
import chat.sphinx.transactions.ui.adapter.TransactionsFooterAdapter
import chat.sphinx.transactions.ui.adapter.TransactionsListAdapter
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class TransactionsFragment: BaseDetailFragment<
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
            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }

        setupTransactions()
    }

    override fun closeDetailsScreen() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.navigator.closeDetailScreen()
        }
    }

    private fun setupTransactions() {
        val addressBookListAdapter = TransactionsListAdapter(viewLifecycleOwner, onStopSupervisor, viewModel)
        val addressBookFooterAdapter = TransactionsFooterAdapter(requireActivity() as InsetterActivity)

        binding.recyclerViewTransactions.apply {
            this.setHasFixedSize(false)
            layoutManager = LinearLayoutManager(binding.root.context)
            adapter = ConcatAdapter(addressBookListAdapter, addressBookFooterAdapter)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    if (!recyclerView.canScrollVertically(1) && newState == RecyclerView.SCROLL_STATE_IDLE) {
                        lifecycleScope.launch(viewModel.mainImmediate) {
                            viewModel.loadMoreTransactions()
                        }
                    }
                }
            })
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: TransactionsViewState) {
        if (viewState is TransactionsViewState.ListMode) {
            if (!viewState.firstPage) {
                return
            }

            binding.apply {
                progressBarTransactionsList.goneIfFalse(
                    viewState.loading
                )

                textViewNoTransactions.goneIfFalse(
                    !viewState.loading && viewState.list.isEmpty()
                )

                recyclerViewTransactions.goneIfFalse(
                    !viewState.loading && viewState.list.isNotEmpty()
                )
            }
        }
    }
}

package chat.sphinx.transactions.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.resources.getString
import chat.sphinx.transactions.R
import chat.sphinx.transactions.databinding.LayoutTransactionHolderBinding
import chat.sphinx.transactions.ui.TransactionsViewModel
import chat.sphinx.transactions.ui.TransactionsViewState
import chat.sphinx.transactions.ui.viewstate.TransactionHolderViewState
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.toSat
import chat.sphinx.wrapper_common.toDateTime
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

internal class TransactionsListAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: TransactionsViewModel
): RecyclerView.Adapter<TransactionsListAdapter.TransactionViewHolder>(), DefaultLifecycleObserver {

    private inner class Diff(
        private val oldList: List<TransactionHolderViewState>,
        private val newList: List<TransactionHolderViewState>,
    ): DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        @Volatile
        var sameList: Boolean = oldListSize == newListSize
            private set

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val same: Boolean =  try {
                oldList[oldItemPosition].let { old ->
                    newList[newItemPosition].let { new ->
                        old.transaction?.id == new.transaction?.id
                    }
                }
            } catch (e: IndexOutOfBoundsException) {
                false
            }

            if (sameList) {
                sameList = same
            }

            return same
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val same: Boolean = try {
                (
                    oldList[oldItemPosition] is TransactionHolderViewState.Failed.Open || newList[oldItemPosition] is TransactionHolderViewState.Failed.Open ||
                    oldList[oldItemPosition] is TransactionHolderViewState.Failed.Closed || newList[oldItemPosition] is TransactionHolderViewState.Failed.Closed ||
                    oldList[oldItemPosition] is TransactionHolderViewState.Incoming || newList[oldItemPosition] is TransactionHolderViewState.Incoming ||
                    oldList[oldItemPosition] is TransactionHolderViewState.Outgoing || newList[oldItemPosition] is TransactionHolderViewState.Outgoing ||
                    oldList[oldItemPosition] is TransactionHolderViewState.Loader || newList[oldItemPosition] is TransactionHolderViewState.Loader
                ) &&
                oldList[oldItemPosition].toString() == newList[newItemPosition].toString()
            } catch (e: IndexOutOfBoundsException) {
                false
            }

            if (sameList) {
                sameList = same
            }

            return same
        }

    }

    private val transactions = ArrayList<TransactionHolderViewState>(viewModel.currentViewState.list)

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.collectViewState { viewState ->
                if (viewState is TransactionsViewState.ListMode) {
                    if (!viewState.loading && viewState.list.isNotEmpty()) {

                        if (transactions.isEmpty()) {
                            transactions.addAll(viewState.list)
                        } else {
                            val diff = Diff(transactions, viewState.list)

                            withContext(viewModel.dispatchers.default) {
                                DiffUtil.calculateDiff(diff)
                            }.let {
                                if (!diff.sameList) {
                                    transactions.clear()
                                    transactions.addAll(viewState.list)
                                }
                            }
                        }
                        this@TransactionsListAdapter.notifyDataSetChanged()
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return transactions.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = LayoutTransactionHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class TransactionViewHolder(
        private val binding: LayoutTransactionHolderBinding
    ): RecyclerView.ViewHolder(binding.root) {

        private var disposable: Disposable? = null
        private var transactionHolderViewState: TransactionHolderViewState? = null

        init {
            binding.root.setOnClickListener {
                transactionHolderViewState?.let { nnTransactionHolderViewState ->
                    lifecycleOwner.lifecycleScope.launch {
                        viewModel.toggleFailed(nnTransactionHolderViewState, absoluteAdapterPosition)
                    }
                }
            }
        }

        fun bind(position: Int) {
            binding.apply {
                val t: TransactionHolderViewState = transactions.getOrNull(position) ?: let {
                    transactionHolderViewState = null
                    return
                }
                transactionHolderViewState = t
                disposable?.dispose()

                val amount = t.transaction?.amount?.toSat()?.asFormattedString() ?: "0"
                val date = t.transaction?.date?.toDateTime()?.value ?: Date(System.currentTimeMillis())

                val hourString = DateTime.getFormathmma().format(date)
                val dayOfMonthString = DateTime.getFormatEEEdd().format(date)
                val dayOfWeekString = DateTime.getFormatMMM().format(date)

                val senderReceiverName = t.messageSenderName ?: "-"

                includeLoadingMoreTransactions.apply {
                    root.goneIfFalse(t is TransactionHolderViewState.Loader)
                }

                includeIncomingTransaction.apply {
                    root.goneIfFalse(t is TransactionHolderViewState.Incoming)

                    textViewTransactionAmount.text = amount
                    textViewTransactionAddress.text = senderReceiverName

                    textViewTransactionHour.text = hourString
                    textViewTransactionDayOfMonth.text = dayOfMonthString
                    textViewTransactionDayOfWeek.text = dayOfWeekString
                }

                includeOutgoingTransaction.apply {
                    root.goneIfFalse(t is TransactionHolderViewState.Outgoing)

                    textViewTransactionAmount.text = amount
                    textViewTransactionAddress.text = senderReceiverName

                    textViewTransactionHour.text = hourString
                    textViewTransactionDayOfMonth.text = dayOfMonthString
                    textViewTransactionDayOfWeek.text = dayOfWeekString
                }

                includeFailedTransaction.apply {
                    root.goneIfFalse(t is TransactionHolderViewState.Failed)

                    textViewTransactionAmount.text = amount
                    textViewTransactionAddress.text = senderReceiverName

                    textViewTransactionHour.text = hourString
                    textViewTransactionDayOfMonth.text = dayOfMonthString
                    textViewTransactionDayOfWeek.text = dayOfWeekString

                    textViewTransactionFailure.text = binding.root.context.getString(R.string.failure_reason, t.transaction?.error_message ?: "")
                    layoutConstraintTransactionFailure.goneIfFalse(t is TransactionHolderViewState.Failed.Open)
                }
            }
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }
}
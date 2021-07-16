package chat.sphinx.transactions.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_network_query_message.model.TransactionDto
import chat.sphinx.transactions.databinding.LayoutTransactionHolderBinding
import chat.sphinx.transactions.ui.TransactionsViewModel
import chat.sphinx.transactions.ui.viewstate.TransactionHolderViewState
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.toDateTime
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            return oldList.size
        }

        @Volatile
        var sameList: Boolean = oldListSize == newListSize
            private set

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val same: Boolean =  try {
                oldList[oldItemPosition].let { old ->
                    newList[newItemPosition].let { new ->
                        old.transaction.id == new.transaction.id
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

                if (transactions.isEmpty()) {
                    transactions.addAll(viewState.list)
                    this@TransactionsListAdapter.notifyDataSetChanged()
                } else {
                    val diff = Diff(transactions, viewState.list)

                    withContext(viewModel.dispatchers.default) {
                        DiffUtil.calculateDiff(diff)
                    }.let {
                        if (!diff.sameList) {
                            transactions.clear()
                            transactions.addAll(viewState.list)
                            this@TransactionsListAdapter.notifyDataSetChanged()
                        }

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

        fun bind(position: Int) {
            binding.apply {
                val t: TransactionHolderViewState = transactions.getOrNull(position) ?: let {
                    transactionHolderViewState = null
                    return
                }
                transactionHolderViewState = t
                disposable?.dispose()

                val amount = t.transaction.amount.toString()
                val date = DateTime.getFormateeemmddhmma().format(t.transaction.date.toDateTime().value)
                val senderReceiverName = t.messageSenderName

                includeIncomingTransaction.apply {
                    root.goneIfFalse(t is TransactionHolderViewState.Incoming)

                    textViewTransactionAmount.text = amount
                    textViewTransactionDate.text = date
                    textViewTransactionSenderReceiver.text = senderReceiverName
                }

                includeOutgoingTransaction.apply {
                    root.goneIfFalse(t is TransactionHolderViewState.Outgoing)

                    textViewTransactionAmount.text = amount
                    textViewTransactionDate.text = date
                    textViewTransactionSenderReceiver.text = senderReceiverName
                }
            }
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }
}
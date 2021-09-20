package chat.sphinx.payment_template.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.payment_template.R
import chat.sphinx.payment_template.databinding.LayoutPaymentTemplateHolderBinding
import chat.sphinx.payment_template.ui.PaymentTemplateViewModel
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.launch
import kotlin.collections.ArrayList

internal class PaymentTemplateAdapter(
    private val recyclerView: RecyclerView,
    private val imageLoader: ImageLoader<ImageView>,
    private val onStopSupervisor: OnStopSupervisor,
    private val viewModel: PaymentTemplateViewModel,
): RecyclerView.Adapter<PaymentTemplateAdapter.PaymentTemplateViewHolder>(), DefaultLifecycleObserver {

    private val paymentTemplates = ArrayList<PaymentTemplate>()

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
//            viewModel.collectChatViewState { viewState ->
//
//                if (dashboardChats.isEmpty()) {
//                    dashboardChats.addAll(viewState.list)
//                    this@PaymentTemplateAdapter.notifyDataSetChanged()
//                }
//            }
        }
    }

    fun setItems(newItems: List<PaymentTemplate>) {
        paymentTemplates.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return paymentTemplates.size + 2
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentTemplateAdapter.PaymentTemplateViewHolder {
        val binding = LayoutPaymentTemplateHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return PaymentTemplateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentTemplateAdapter.PaymentTemplateViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class PaymentTemplateViewHolder(
        private val binding: LayoutPaymentTemplateHolderBinding
    ): RecyclerView.ViewHolder(binding.root), DefaultLifecycleObserver {

        private var paymentTemplate: PaymentTemplate? = null

        init {
            binding.root.setOnClickListener {
                paymentTemplate?.let { nnPaymentTemplate ->

                }
            }
        }

        fun bind(position: Int) {
            binding.apply {
                val itemWidth = recyclerView.context.resources.getDimension(R.dimen.payment_template_recycler_view_item_width).toInt()
                val paddingItemWidth = ((recyclerView.width - itemWidth) / 2).toInt()

                if (position == 0 || position == paymentTemplates.size + 1) {
                    root.updateLayoutParams { width = paddingItemWidth }

                    imageViewTemplate.gone
                } else {
                    root.updateLayoutParams { width = itemWidth }

                    imageViewTemplate.visible

                    val paymentT: PaymentTemplate = paymentTemplates.getOrNull(position - 1) ?: let {
                        paymentTemplate = null
                        return
                    }
                    paymentTemplate = paymentT
                }
            }
        }
    }
}

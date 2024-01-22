package chat.sphinx.payment_template.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.payment_template.R
import chat.sphinx.payment_template.databinding.LayoutPaymentTemplateHolderBinding
import chat.sphinx.payment_template.ui.PaymentTemplateViewModel
import chat.sphinx.wrapper_common.payment.PaymentTemplate
import chat.sphinx.wrapper_meme_server.AuthenticationToken
import chat.sphinx.wrapper_meme_server.headerKey
import chat.sphinx.wrapper_meme_server.headerValue
import chat.sphinx.wrapper_meme_server.toAuthenticationToken
import chat.sphinx.wrapper_message_media.token.MediaHost
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.collections.ArrayList

internal class PaymentTemplateAdapter(
    private val recyclerView: RecyclerView,
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: PaymentTemplateViewModel,
): RecyclerView.Adapter<PaymentTemplateAdapter.PaymentTemplateViewHolder>(), DefaultLifecycleObserver {

    companion object {
        const val PADDING_ITEMS_COUNT: Int = 2
        const val NO_TEMPLATE_ITEMS_COUNT: Int = 1
    }

    private var paymentTemplates = ArrayList<PaymentTemplate>()

    fun setItems(newItems: List<PaymentTemplate>) {
        paymentTemplates.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return paymentTemplates.size + PADDING_ITEMS_COUNT + NO_TEMPLATE_ITEMS_COUNT
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

        private var holderJob: Job? = null
        private var holderDisposable: Disposable? = null

        fun bind(position: Int) {

            holderJob?.cancel()
            holderDisposable?.dispose()

            binding.apply {

                val itemWidth = recyclerView.context.resources.getDimension(R.dimen.payment_template_recycler_view_item_width).toInt()
                val paddingItemWidth = ((recyclerView.width - itemWidth) / 2)

                val isStartPaddingItem = position == 0
                val isEndPaddingItem = position == paymentTemplates.size + 2

                if (isStartPaddingItem || isEndPaddingItem) {
                    root.updateLayoutParams { width = paddingItemWidth }

                    imageViewTemplate.gone
                } else {
                    root.updateLayoutParams { width = itemWidth }

                    imageViewTemplate.visible

                    paymentTemplates.getOrNull(position - 2)?.let { paymentTemplate ->
                        paymentTemplate.getTemplateUrl(MediaHost.DEFAULT.value).let { url ->

                            val token = AuthenticationToken(paymentTemplate.token)

                            lifecycleOwner.lifecycleScope.launch(viewModel.dispatchers.mainImmediate) {
                                imageLoader.load(
                                    imageViewTemplate,
                                    url,
                                    ImageLoaderOptions.Builder()
                                        .transformation(Transformation.CircleCrop)
                                        .addHeader(token.headerKey, token.headerValue)
                                        .build()
                                ).also {  holderDisposable = it }
                            }.let { job ->
                                holderJob = job
                            }
                        }
                    } ?: run {
                        lifecycleOwner.lifecycleScope.launch(viewModel.dispatchers.mainImmediate) {
                            imageLoader.load(
                                imageViewTemplate,
                                -1,
                                ImageLoaderOptions.Builder().build()
                            ).also {  holderDisposable = it }
                        }.let { job ->
                            holderJob = job
                        }
                    }
                }
            }
        }
    }
}

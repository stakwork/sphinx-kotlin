package chat.sphinx.payment_template.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.payment_template.R
import chat.sphinx.payment_template.databinding.FragmentPaymentTemplateBinding
import chat.sphinx.payment_template.ui.adapter.PaymentTemplateAdapter
import chat.sphinx.payment_template.ui.viewstate.PaymentTemplateViewState
import chat.sphinx.payment_template.ui.viewstate.TemplateImagesViewState
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@AndroidEntryPoint
internal class PaymentTemplateFragment: SideEffectFragment<
        Context,
        PaymentTemplateSideEffect,
        PaymentTemplateViewState,
        PaymentTemplateViewModel,
        FragmentPaymentTemplateBinding
    >(R.layout.fragment_payment_template) {

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    private val args: PaymentTemplateFragmentArgs by navArgs()
    override val viewModel: PaymentTemplateViewModel by viewModels()
    override val binding: FragmentPaymentTemplateBinding by viewBinding(
        FragmentPaymentTemplateBinding::bind
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            includePaymentTemplateHeader.apply {
                textViewDetailScreenHeaderName.text = getString(R.string.payment_template_header_name)

                textViewDetailScreenClose.setOnClickListener {
                    lifecycleScope.launch(viewModel.mainImmediate) {
                        viewModel.navigator.closeDetailScreen()
                    }
                }
                textViewDetailScreenHeaderNavBack.visible
                textViewDetailScreenHeaderNavBack.setOnClickListener {
                    lifecycleScope.launch(viewModel.mainImmediate) {
                        viewModel.navigator.popBackStack()
                    }
                }
            }

            textViewAmount.text = args.argAmount.toString()
            textViewMessage.text = args.argMessage

            buttonConfirm.setOnClickListener {
                sendPayment()
            }
        }

        setupPaymentTemplateRecycler()
        setupFragmentLayout()
    }

    private fun sendPayment() {
        binding.recyclerViewPaymentTemplate.apply {
            snapHelper.findSnapView(layoutManager)?.let { centerView ->
                layoutManager?.getPosition(centerView)?.let { position ->

                }
//                viewModel.sendPayment()
            }
        }
    }

    private var snapPosition = RecyclerView.NO_POSITION
    private val snapHelper: SnapHelper = LinearSnapHelper()

    private fun setupPaymentTemplateRecycler() {
        binding.recyclerViewPaymentTemplate.apply {
            val paymentTemplateAdapter = PaymentTemplateAdapter(
                this,
                imageLoader,
                onStopSupervisor,
                viewModel,
            )

            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = paymentTemplateAdapter

            snapHelper.attachToRecyclerView(this)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    snapHelper.findSnapView(layoutManager)?.let { centerView ->
                        layoutManager?.getPosition(centerView)?.let { position ->
                            if (snapPosition != position) {
                                changeSelectedTemplate(position)
                            }
                        }
                    }
                }
            })
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.loadTemplateImages()
        }
    }

    private fun changeSelectedTemplate(position: Int) {
        binding.apply {
            val viewHolder = recyclerViewPaymentTemplate.findViewHolderForAdapterPosition(position)
            if (viewHolder is PaymentTemplateAdapter.PaymentTemplateViewHolder) {
                val image = viewHolder.binding.imageViewTemplate.drawable
                imageViewSelectedPaymentTemplate.setImageDrawable(image)

                viewModel.selectTemplate(position)
            }
        }
    }

    private fun setupFragmentLayout() {
        (requireActivity() as InsetterActivity).addNavigationBarPadding(
            binding.root
        )
    }

    override suspend fun onSideEffectCollect(sideEffect: PaymentTemplateSideEffect) {
        sideEffect.execute(requireActivity())
    }

    override suspend fun onViewStateFlowCollect(viewState: PaymentTemplateViewState) {
        when (viewState) {
            is PaymentTemplateViewState.Idle -> {}

            is PaymentTemplateViewState.ProcessingPayment -> {
                binding.progressBarConfirm.visible
            }
            is PaymentTemplateViewState.PaymentFailed -> {
                binding.progressBarConfirm.gone
            }
        }
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.templateImagesViewStateContainer.collect { viewState ->
                @Exhaustive
                when (viewState) {
                    is TemplateImagesViewState.LoadingTemplateImages -> {
                        binding.layoutConstraintProgressBarLoadingTemplates.visible
                    }
                    is TemplateImagesViewState.TemplateImages -> {
                        binding.layoutConstraintProgressBarLoadingTemplates.gone

                        if (binding.recyclerViewPaymentTemplate.adapter is PaymentTemplateAdapter) {

                            (binding.recyclerViewPaymentTemplate.adapter as PaymentTemplateAdapter)
                                .setItems(viewState.templates)
                        }
                    }
                }
            }
        }
    }
}
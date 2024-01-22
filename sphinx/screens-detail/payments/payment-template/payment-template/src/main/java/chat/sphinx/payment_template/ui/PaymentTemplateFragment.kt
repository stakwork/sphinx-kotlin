package chat.sphinx.payment_template.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.payment_template.R
import chat.sphinx.payment_template.databinding.FragmentPaymentTemplateBinding
import chat.sphinx.payment_template.ui.adapter.PaymentTemplateAdapter
import chat.sphinx.payment_template.ui.viewstate.PaymentTemplateViewState
import chat.sphinx.payment_template.ui.viewstate.SelectedTemplateViewState
import chat.sphinx.payment_template.ui.viewstate.TemplateImagesViewState
import chat.sphinx.wrapper_meme_server.AuthenticationToken
import chat.sphinx.wrapper_meme_server.headerKey
import chat.sphinx.wrapper_meme_server.headerValue
import chat.sphinx.wrapper_message_media.token.MediaHost
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.Job
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

    private var currentTemplateViewStateJob: Job? = null
    private var currentTemplateViewStateDisposable: Disposable? = null

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
                viewModel.sendPayment()
            }
        }

        setupPaymentTemplateRecycler()
        setupFragmentLayout()
    }

    private var snapPosition = RecyclerView.NO_POSITION
    private fun setupPaymentTemplateRecycler() {
        binding.recyclerViewPaymentTemplate.apply {
            val paymentTemplateAdapter = PaymentTemplateAdapter(
                this,
                imageLoader,
                viewLifecycleOwner,
                viewModel,
            )

            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = paymentTemplateAdapter

            val snapHelper: SnapHelper = LinearSnapHelper()
            snapHelper.attachToRecyclerView(this)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    snapHelper.findSnapView(layoutManager)?.let { centerView ->
                        layoutManager?.getPosition(centerView)?.let { position ->
                            if (snapPosition != position) {
                                snapPosition = position
                                //Subtracting padding item and no template item from position
                                viewModel.selectTemplate(position - 2)
                            }
                        }
                    }
                }
            })
        }

        viewModel.loadTemplateImages()
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

    private var templateImagesViewState: TemplateImagesViewState? = null
    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.templateImagesViewStateContainer.collect { viewState ->
                if (templateImagesViewState != viewState) {
                    templateImagesViewState = viewState

                    @Exhaustive
                    when (viewState) {
                        is TemplateImagesViewState.LoadingTemplateImages -> {
                            binding.layoutConstraintProgressBarLoadingTemplates.visible
                        }
                        is TemplateImagesViewState.TemplateImages -> {
                            binding.layoutConstraintProgressBarLoadingTemplates.gone

                            binding.recyclerViewPaymentTemplate.apply {

                                (adapter as PaymentTemplateAdapter).setItems(viewState.templates)

                                //scrolling to first template item
                                scrollBy(
                                    context.resources.getDimension(R.dimen.payment_template_recycler_view_item_width).toInt(),
                                    0
                                )
                            }
                        }
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.selectedTemplateViewStateContainer.collect { viewState ->

                currentTemplateViewStateJob?.cancel()
                currentTemplateViewStateDisposable?.dispose()

                @Exhaustive
                when (viewState) {
                    is SelectedTemplateViewState.Idle -> {
                        lifecycleScope.launch(viewModel.mainImmediate) {
                            binding.imageViewSelectedPaymentTemplate.setImageDrawable(
                                ContextCompat.getDrawable(binding.root.context, R.drawable.ic_no_template_with_padding)
                            )
                        }.let { job ->
                            currentTemplateViewStateJob = job
                        }
                    }
                    is SelectedTemplateViewState.SelectedTemplate -> {
                        val template = viewState.template

                        template.getTemplateUrl(MediaHost.DEFAULT.value).let { url ->
                            val token = AuthenticationToken(template.token)

                            lifecycleScope.launch(viewModel.mainImmediate) {
                                imageLoader.load(
                                    binding.imageViewSelectedPaymentTemplate,
                                    url,
                                    ImageLoaderOptions.Builder()
                                        .addHeader(token.headerKey, token.headerValue)
                                        .build()
                                ).also { currentTemplateViewStateDisposable = it }
                            }.let { job ->
                                currentTemplateViewStateJob = job
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        templateImagesViewState = null
    }
}
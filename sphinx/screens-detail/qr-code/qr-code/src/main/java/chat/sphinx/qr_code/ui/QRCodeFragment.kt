package chat.sphinx.qr_code.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.qr_code.R
import chat.sphinx.qr_code.databinding.FragmentQrCodeBinding
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import chat.sphinx.share_qr_code.BottomMenuShareQRCode
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class QRCodeFragment: SideEffectDetailFragment<
        Context,
        NotifySideEffect,
        QRCodeViewState,
        QRCodeViewModel,
        FragmentQrCodeBinding
        >(R.layout.fragment_qr_code)
{
    override val binding: FragmentQrCodeBinding by viewBinding(FragmentQrCodeBinding::bind)
    override val viewModel: QRCodeViewModel by viewModels()

    private val bottomMenuShareQRCode: BottomMenuShareQRCode by lazy(LazyThreadSafetyMode.NONE) {
        BottomMenuShareQRCode(
            onStopSupervisor,
            viewModel
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel
    }

    override fun closeDetailsScreen() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.navigator.closeDetailScreen()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(binding.layoutConstraintQrCodeFragment)

        binding.apply {
            includeQrCodeHeader.textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }

            includeQrCodeHeader.textViewDetailScreenHeaderNavBack.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.popBackStack()
                }
            }

            buttonQrCodeCopy.setOnClickListener {
                viewModel.copyCodeToClipboard()
            }

            buttonQrCodeShare.setOnClickListener {
                viewModel.shareQRCodeMenuHandler.viewStateContainer.updateViewState(
                    MenuBottomViewState.Open
                )
            }

            bottomMenuShareQRCode.initialize(
                includeLayoutMenuBottomShareQrCode,
                viewLifecycleOwner
            )
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: QRCodeViewState) {
        binding.apply {
            includeQrCodeHeader.apply header@ {
                this@header.textViewDetailScreenHeaderNavBack.goneIfFalse(viewState.showBackButton)
                this@header.textViewDetailScreenHeaderName.text = viewState.viewTitle
            }

            qrCodeLabel.text = viewState.qrText

            viewState.qrBitmap?.let {
                qrCode.setImageBitmap(it)
            }

            viewState.description?.let {
                textViewQrCodeDescription.text = it
                textViewQrCodeDescription.visible
            }

            if (viewState.paid && layoutConstraintInvoicePaid.isGone) {
                layoutConstraintInvoicePaid.apply {
                    alpha = 0.0F
                    visible
                    animate().alpha(1.0F)
                }
            }
            layoutConstraintInvoicePaid.goneIfFalse(viewState.paid)
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: NotifySideEffect) {
        sideEffect.execute(binding.root.context)
    }
}

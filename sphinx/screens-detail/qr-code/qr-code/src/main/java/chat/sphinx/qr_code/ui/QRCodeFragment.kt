package chat.sphinx.qr_code.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.qr_code.R
import chat.sphinx.qr_code.databinding.FragmentQrCodeBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class QRCodeFragment: SideEffectFragment<
        Context,
        NotifySideEffect,
        QRCodeViewState,
        QRCodeViewModel,
        FragmentQrCodeBinding
        >(R.layout.fragment_qr_code)
{
    override val binding: FragmentQrCodeBinding by viewBinding(FragmentQrCodeBinding::bind)
    override val viewModel: QRCodeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(binding.layoutConstraintQrCodeFragment)

        binding.includeQrCodeHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.qr_code_header_name)

            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }

            textViewDetailScreenHeaderNavBack.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.popBackStack()
                }
            }
        }

        binding.buttonQrCodeCopy.setOnClickListener {
            viewModel.copyCodeToClipboard()
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: QRCodeViewState) {
        binding.includeQrCodeHeader.textViewDetailScreenHeaderNavBack
            .goneIfFalse(viewState.showBackButton)
        binding.qrCodeLabel.text = viewState.qrText
        viewState.qrBitmap?.let { binding.qrCode.setImageBitmap(it) }
    }

    override suspend fun onSideEffectCollect(sideEffect: NotifySideEffect) {
        sideEffect.execute(binding.root.context)
    }
}

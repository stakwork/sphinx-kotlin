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
        binding.apply {
            includeQrCodeHeader.apply header@ {
                this@header.textViewDetailScreenHeaderNavBack
                    .goneIfFalse(viewState.showBackButton)
                this@header.textViewDetailScreenHeaderName.text = viewState.viewTitle
            }

            qrCodeLabel.text = viewState.qrText

            viewState.qrBitmap?.let {
                qrCode.setImageBitmap(it)
            }

            viewState.description?.let {
                textViewQrCodeDescription.text = it
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: NotifySideEffect) {
        sideEffect.execute(binding.root.context)
    }
}

package chat.sphinx.qr_code.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.qr_code.R
import chat.sphinx.qr_code.databinding.FragmentQrCodeBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
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
    private val args: QRCodeFragmentArgs by navArgs()
    override val binding: FragmentQrCodeBinding by viewBinding(FragmentQrCodeBinding::bind)
    override val viewModel: QRCodeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as InsetterActivity).addNavigationBarPadding(binding.layoutConstraintQrCodeFragment)

        binding.qrCodeLabel.text = args.qrText

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(args.qrText, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        binding.qrCode.setImageBitmap(bitmap)

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
            val textToCopy = binding.qrCodeLabel.text
            val clipboardManager = binding.root.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("text", textToCopy)
            clipboardManager.setPrimaryClip(clipData)

            lifecycleScope.launch(viewModel.mainImmediate) {
                viewModel.submitSideEffect(
                    NotifySideEffect(getString(R.string.qr_code_notify_copied))
                )
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: QRCodeViewState) {
        binding.includeQrCodeHeader.textViewDetailScreenHeaderNavBack.apply {
            @Exhaustive
            when (viewState) {
                is QRCodeViewState.LayoutVisibility -> {
                    goneIfFalse(viewState.showBackButton)
                }
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: NotifySideEffect) {
        sideEffect.execute(binding.root.context)
    }
}

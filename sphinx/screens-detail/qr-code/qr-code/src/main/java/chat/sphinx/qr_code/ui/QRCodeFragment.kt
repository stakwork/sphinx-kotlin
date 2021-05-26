package chat.sphinx.qr_code.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.qr_code.R
import chat.sphinx.qr_code.databinding.FragmentQrCodeBinding
import chat.sphinx.qr_code.navigation.BackType
import chat.sphinx.resources.SphinxToastUtils
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.invisible
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.android_feature_viewmodel.updateViewState

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
        // have to call it here so it gets injected and can
        // catch the request asap
        viewModel
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (viewModel.args.argShowBackArrow) {
            viewModel.updateViewState(QRCodeViewState.ShowNavBackButton)
        } else {
            viewModel.updateViewState(QRCodeViewState.HideNavBackButton)
        }

        binding.qrCodeLabel.text = viewModel.args.qrText

        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(viewModel.args.qrText, BarcodeFormat.QR_CODE, 512, 512)
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
                viewModel.goBack(BackType.CloseDetailScreen)
            }
            textViewDetailScreenHeaderNavBack.setOnClickListener {
                viewModel.goBack(BackType.PopBackStack)
            }
        }

        binding.buttonQrCodeCopy.setOnClickListener {
            val textToCopy = binding.qrCodeLabel.text
            val clipboardManager = binding.root.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("text", textToCopy)
            clipboardManager.setPrimaryClip(clipData)

            SphinxToastUtils().show(binding.root.context, "Public Key copied to clipboard")
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: QRCodeViewState) {
        binding.includeQrCodeHeader.textViewDetailScreenHeaderNavBack.apply {
            @Exhaustive
            when (viewState) {
                QRCodeViewState.HideNavBackButton -> {
                    invisible
                }
                QRCodeViewState.ShowNavBackButton -> {
                    visible
                }
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: NotifySideEffect) {
        sideEffect.execute(binding.root.context)
    }
}

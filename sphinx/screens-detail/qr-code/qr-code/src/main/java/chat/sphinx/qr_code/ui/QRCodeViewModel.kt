package chat.sphinx.qr_code.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_socket_io.SocketIOManager
import chat.sphinx.concept_socket_io.SphinxSocketIOMessage
import chat.sphinx.concept_socket_io.SphinxSocketIOMessageListener
import chat.sphinx.qr_code.R
import chat.sphinx.qr_code.navigation.QRCodeNavigator
import chat.sphinx.wrapper_common.util.isValidBech32
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class QRCodeViewModel @Inject constructor(
    private val app: Application,
    val navigator: QRCodeNavigator,
    private val socketIOManager: SocketIOManager,
    dispatchers: CoroutineDispatchers,
    handle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        NotifySideEffect,
        QRCodeViewState
        >(
            dispatchers,
            handle.navArgs<QRCodeFragmentArgs>().let {
                QRCodeViewState(
                    it.value.argShowBackArrow,
                    it.value.viewTitle,
                    it.value.qrText,
                    null,
                    it.value.argDescription,
                )
            },
        ),
    SphinxSocketIOMessageListener
{

    companion object {
        private const val BITMAP_XY = 512
    }

    private val args: QRCodeFragmentArgs by handle.navArgs()

    init {
        socketIOManager.addListener(this)

        viewModelScope.launch(default) {
            val writer = QRCodeWriter()
            val qrText = if (args.qrText.isValidBech32()) {
                // Bech32 strings must be upper cased when show in QR codes
                // https://github.com/lightningnetwork/lightning-rfc/blob/master/11-payment-encoding.md#requirements
                args.qrText.uppercase()
            } else {
                args.qrText
            }
            val bitMatrix = writer.encode(
                qrText,
                BarcodeFormat.QR_CODE,
                BITMAP_XY,
                BITMAP_XY
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }

            updateViewState(
                QRCodeViewState(
                    currentViewState.showBackButton,
                    currentViewState.viewTitle,
                    currentViewState.qrText,
                    bitmap,
                    currentViewState.description,
                )
            )
        }
    }

    fun copyCodeToClipboard() {
        (app.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.let { manager ->
            val clipData = ClipData.newPlainText("text", args.qrText)
            manager.setPrimaryClip(clipData)

            viewModelScope.launch(mainImmediate) {
                submitSideEffect(
                    NotifySideEffect(app.getString(R.string.qr_code_notify_copied, args.viewTitle))
                )
            }
        }
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    override suspend fun onSocketIOMessageReceived(msg: SphinxSocketIOMessage) {
        if (msg is SphinxSocketIOMessage.Type.InvoicePayment) {
            if (args.qrText == msg.dto.invoice) {
                updateViewState(
                    QRCodeViewState(
                        currentViewState.showBackButton,
                        currentViewState.viewTitle,
                        currentViewState.qrText,
                        currentViewState.qrBitmap,
                        currentViewState.description,
                        true
                    )
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        socketIOManager.removeListener(this)
    }
}

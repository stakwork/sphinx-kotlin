package chat.sphinx.qr_code.ui

import android.app.Application
import android.content.*
import android.graphics.Bitmap
import android.graphics.Color
import android.provider.MediaStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_socket_io.SocketIOManager
import chat.sphinx.concept_socket_io.SphinxSocketIOMessage
import chat.sphinx.concept_socket_io.SphinxSocketIOMessageListener
import chat.sphinx.qr_code.R
import chat.sphinx.qr_code.navigation.QRCodeNavigator
import chat.sphinx.share_qr_code.ShareQRCodeMenuHandler
import chat.sphinx.share_qr_code.ShareQRCodeMenuViewModel
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
import io.matthewnelson.concept_media_cache.MediaCacheHandler
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
internal class QRCodeViewModel @Inject constructor(
    private val app: Application,
    val navigator: QRCodeNavigator,
    private val socketIOManager: SocketIOManager,
    private val mediaCacheHandler: MediaCacheHandler,
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
    SphinxSocketIOMessageListener,
    ShareQRCodeMenuViewModel
{

    override val shareQRCodeMenuHandler: ShareQRCodeMenuHandler by lazy {
        ShareQRCodeMenuHandler()
    }

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

    override fun shareCodeThroughTextIntent(): Intent {
        val sharingIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, currentViewState.qrText)
        }

        return Intent.createChooser(
            sharingIntent,
            app.getString(R.string.share_qr_code_as_text)
        )
    }

    override fun shareCodeThroughImageIntent(): Intent? {
        return currentViewState.qrBitmap?.let { qrBitmap ->
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.TITLE, currentViewState.qrText)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }
            val mediaStorageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val inputStream = qrBitmap.toInputStream()

            app.contentResolver.insert(mediaStorageUri, contentValues)?.let { savedFileUri ->

                try {
                    app.contentResolver.openOutputStream(savedFileUri).use { savedFileOutputStream ->
                        if (savedFileOutputStream != null) {
                            inputStream.copyTo(savedFileOutputStream, 1024)

                            val sharingIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/jpeg"
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                putExtra(Intent.EXTRA_TEXT, currentViewState.qrText)
                                putExtra(Intent.EXTRA_STREAM, savedFileUri)
                            }

                            return Intent.createChooser(
                                sharingIntent,
                                app.getString(R.string.share_qr_code_as_image_plus_text)
                            )
                        }
                    }
                } catch (e: Exception) {
                }

                try {
                    app.contentResolver.delete(savedFileUri, null, null)
                } catch (fileE: Exception) {
                }
            }

            null
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun Bitmap.toInputStream(): InputStream {
    val stream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 100, stream)
    val imageInByte: ByteArray = stream.toByteArray()
    return ByteArrayInputStream(imageInByte)
}
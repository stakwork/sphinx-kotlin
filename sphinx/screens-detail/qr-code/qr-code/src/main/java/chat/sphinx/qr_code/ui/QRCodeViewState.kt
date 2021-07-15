package chat.sphinx.qr_code.ui

import android.graphics.Bitmap
import io.matthewnelson.concept_views.viewstate.ViewState

internal data class QRCodeViewState(
    val showBackButton: Boolean,
    val viewTitle: String,
    val qrText: String,
    val qrBitmap: Bitmap?,
    val description: String? = null,
    val paid: Boolean = false,
): ViewState<QRCodeViewState>()


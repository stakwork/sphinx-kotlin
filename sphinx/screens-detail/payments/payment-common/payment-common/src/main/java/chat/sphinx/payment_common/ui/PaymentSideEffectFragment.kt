package chat.sphinx.payment_common.ui

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher

interface PaymentSideEffectFragment {
    val paymentFragmentContext: Context
    val fragmentActivity: Activity?
}
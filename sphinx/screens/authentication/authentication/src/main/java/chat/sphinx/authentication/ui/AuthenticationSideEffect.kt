package chat.sphinx.authentication.ui

import android.view.HapticFeedbackConstants
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentActivity
import chat.sphinx.authentication.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.concept_views.sideeffect.SideEffect
import io.matthewnelson.android_feature_toast_utils.show

internal sealed class AuthenticationSideEffect: SideEffect<FragmentActivity>() {

    sealed class Notify(
        @StringRes private val toastMessageResId: Int
    ): AuthenticationSideEffect() {

        @Suppress("DEPRECATION")
        override suspend fun execute(value: FragmentActivity) {
            SphinxToastUtils().show(value, toastMessageResId)
        }

        object WrongPin: Notify(R.string.wrong_pin)
        object OneMoreAttemptBeforeLockout: Notify(R.string.wrong_pin_one_more_attempt)
        object PinDoesNotMatch: Notify(R.string.wrong_pin_pin_does_not_match)
    }

    object ProduceHapticFeedback: AuthenticationSideEffect() {
        override suspend fun execute(value: FragmentActivity) {
            value.window.decorView.performHapticFeedback(
                HapticFeedbackConstants.LONG_PRESS,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
    }
}

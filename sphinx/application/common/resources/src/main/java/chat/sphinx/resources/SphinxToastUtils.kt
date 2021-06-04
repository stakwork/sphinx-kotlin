package chat.sphinx.resources

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import io.matthewnelson.android_feature_toast_utils.R as R_toast_utils
import io.matthewnelson.android_feature_toast_utils.ToastUtils

class SphinxToastUtils(
    toastLengthLong: Boolean = false,
    @DrawableRes toastBackground: Int = R_toast_utils.drawable.toast_utils_default_background,
    @ColorRes toastBackgroundTint: Int = R.color.primaryGreen,
    @ColorRes textColor: Int = android.R.color.white,
    @DrawableRes imageBackground: Int? = null,
    @DrawableRes image: Int? = DEFAULT_ICON
): ToastUtils(
    toastLengthLong,
    toastBackground,
    toastBackgroundTint,
    textColor,
    imageBackground,
    image
) {
    companion object {
        @get:DrawableRes
        val DEFAULT_ICON: Int
            get() = R.drawable.sphinx_white_notification
    }
}

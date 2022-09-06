package chat.sphinx.notification_level.ui

import android.content.Context
import chat.sphinx.notification_level.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class  NotificationLevelSideEffect: SideEffect<Context>()  {
    class Notify(
        private val msg: String,
        private val notificationLengthLong: Boolean = false
    ): NotificationLevelSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(toastLengthLong = notificationLengthLong).show(value, msg)
        }
    }
}
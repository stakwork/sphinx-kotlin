package chat.sphinx.tribe_members_list.ui

import android.content.Context
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class TribeMembersListSideEffect: SideEffect<Context>() {
    class Notify(
        private val msg: String,
        private val notificationLengthLong: Boolean = true
    ): TribeMembersListSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(toastLengthLong = notificationLengthLong).show(value, msg)
        }
    }
}

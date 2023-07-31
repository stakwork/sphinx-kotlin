package chat.sphinx.threads.ui

import android.content.Context
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal class ThreadsSideEffect(val value: String): SideEffect<Context>() {
    override suspend fun execute(value: Context) {
        SphinxToastUtils().show(value, this.value)
    }
}

package chat.sphinx.chat_common.ui.activity

import android.content.Context
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class FullscreenVideoSideEffect : SideEffect<Context>()  {

    object FailedToLoadVideo: FullscreenVideoSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(false).show(value, "Failed to load Video")
        }
    }

}
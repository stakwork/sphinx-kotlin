package chat.sphinx.camera.ui

import androidx.fragment.app.FragmentActivity
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class CameraSideEffect: SideEffect<FragmentActivity>() {
    class Notify(private val msg: String): CameraSideEffect() {
        override suspend fun execute(value: FragmentActivity) {
            SphinxToastUtils().show(value, msg)
        }
    }
}

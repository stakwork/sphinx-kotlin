package chat.sphinx.tribe_badge.ui

import android.content.Context
import androidx.annotation.StringRes
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.tribe_badge.R
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class TribeBadgesSideEffect : SideEffect<Context>()  {
    object BadgeAlreadyExists: TribeBadgesSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.badge_already_exist)
        }
    }
}

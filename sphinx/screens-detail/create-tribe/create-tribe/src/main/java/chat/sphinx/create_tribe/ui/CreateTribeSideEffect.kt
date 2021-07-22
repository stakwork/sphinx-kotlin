package chat.sphinx.create_tribe.ui

import android.content.Context
import chat.sphinx.create_tribe.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

internal sealed class  CreateTribeSideEffect: SideEffect<Context>()  {
    object NameAndDescriptionRequired: CreateTribeSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.name_and_description_required)
        }
    }

    object FailedToCreateTribe: CreateTribeSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.failed_to_create_tribe)
        }
    }
}
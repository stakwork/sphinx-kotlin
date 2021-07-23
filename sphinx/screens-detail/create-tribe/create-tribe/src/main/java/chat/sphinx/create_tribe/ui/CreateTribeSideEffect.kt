package chat.sphinx.create_tribe.ui

import android.app.AlertDialog
import android.content.Context
import chat.sphinx.concept_repository_chat.model.CreateTribe
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

    object FailedToProcessImage: CreateTribeSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.failed_to_process_image)
        }
    }

    class AlertSelectTags(
        private val createTribeBuilder: CreateTribe.Builder,
        private val callback: () -> Unit
    ): CreateTribeSideEffect() {
        override suspend fun execute(value: Context) {
            val items = createTribeBuilder.tags.map {
                it.name
            }.toTypedArray()
            val selected = createTribeBuilder.tags.map {
                it.isSelected
            }.toBooleanArray()

            val builder = AlertDialog.Builder(value)
            builder.setMultiChoiceItems(items, selected) { _, which, isChecked ->
                createTribeBuilder.selectTag(which, isChecked)
            }
            builder.setOnDismissListener {
                callback()
            }
            builder.show()
        }
    }
}
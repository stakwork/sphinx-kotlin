package chat.sphinx.create_tribe.ui

import android.app.AlertDialog
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.ListView
import androidx.appcompat.content.res.AppCompatResources
import chat.sphinx.concept_repository_chat.model.CreateTribe
import chat.sphinx.create_tribe.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect

fun CreateTribe.Builder.adapter(context: Context) = object : ArrayAdapter<CreateTribe.Builder.Tag?>(
    context, R.layout.layout_tags, R.id.text_view_tag_item, tags
) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        val tag = tags[position]

        val textView = view.findViewById<CheckedTextView>(R.id.text_view_tag_item)
        textView.isChecked = tag.isSelected
        textView.text = tag.toString()

        AppCompatResources.getDrawable(context, tag.image)?.let { icon ->
            icon.setBounds(0, 0, 80, 80)
            textView.setCompoundDrawables(icon, null, null, null)
        }
        return view
    }
}


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

    object FailedToUpdateTribe: CreateTribeSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.failed_to_create_tribe)
        }
    }

    object FailedToLoadTribe: CreateTribeSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.failed_to_load_tribe)
        }
    }

    object FailedToProcessImage: CreateTribeSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.failed_to_process_image)
        }
    }

    object InvalidUrl: CreateTribeSideEffect() {
        override suspend fun execute(value: Context) {
            SphinxToastUtils(true).show(value, R.string.invalid_url)
        }
    }

    class AlertSelectTags(
        private val createTribeBuilder: CreateTribe.Builder,
        private val callback: () -> Unit
    ): CreateTribeSideEffect() {
        override suspend fun execute(value: Context) {
            val adapter = createTribeBuilder.adapter(value)

            val builder = AlertDialog.Builder(value, R.style.AlertDialogTheme)
            builder.setAdapter(adapter, null)
            builder.setOnDismissListener {
                callback()
            }

            val dialog = builder.create()
            dialog.listView.background = AppCompatResources.getDrawable(value, R.color.body)
            dialog.listView.setOnItemClickListener { _, view, which, _ ->
                val tag = createTribeBuilder.tags[which]
                tag.isSelected = !tag.isSelected

                val textView = view.findViewById<CheckedTextView>(R.id.text_view_tag_item)
                textView.isChecked = tag.isSelected
            }
            dialog.listView.choiceMode = ListView.CHOICE_MODE_MULTIPLE_MODAL

            dialog.show()
        }
    }
}
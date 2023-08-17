package chat.sphinx.chat_tribe.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import chat.sphinx.chat_tribe.R

class MessageMentionsAdapter(
    context: Context,
    mentions: MutableList<String>?
) : ArrayAdapter<String>(context, 0, mentions as MutableList<String>) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView: View? = convertView
        val mention: String? = getItem(position)

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.layout_message_mention_item, parent, false)
        }

        val mentionTextView: TextView = convertView?.findViewById(R.id.text_view_message_mention) as TextView
        mentionTextView.text = mention

        convertView?.let {
            return it
        } ?: run {
            return super.getView(position, convertView, parent)
        }
    }
}
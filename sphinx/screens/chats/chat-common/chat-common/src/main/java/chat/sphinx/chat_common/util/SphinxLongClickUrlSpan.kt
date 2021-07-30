package chat.sphinx.chat_common.util

import android.view.View

open class SphinxLongClickUrlSpan(
    url: String?,
    onLongClickListener: View.OnLongClickListener,
    private val onClickListener: OnClickListener
): LongClickUrlSpan(url, onLongClickListener) {

    override fun onClick(widget: View) {
        onClickListener.onClick(url)
    }

    interface OnClickListener {
        /**
         * Called when a view has been clicked.
         *
         * @param url The url that was clicked.
         */
        fun onClick(url: String?)
    }
}
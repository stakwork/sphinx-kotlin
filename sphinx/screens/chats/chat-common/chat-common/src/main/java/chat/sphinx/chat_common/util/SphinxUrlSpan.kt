package chat.sphinx.chat_common.util

import android.text.style.URLSpan
import android.view.View
import chat.sphinx.wrapper_common.lightning.isValidLightningNodePubKey
import chat.sphinx.wrapper_common.lightning.isValidLightningPaymentRequest
import chat.sphinx.wrapper_common.lightning.isValidVirtualLightningNodePubKey
import chat.sphinx.wrapper_common.tribe.isValidTribeJoinLink
import java.util.concurrent.atomic.AtomicInteger

inline val String.isSphinxUrl: Boolean
    get() = isValidLightningPaymentRequest ||
            isValidLightningNodePubKey ||
            isValidVirtualLightningNodePubKey ||
            isValidTribeJoinLink

open class SphinxUrlSpan(
    url: String?,
    private val onInteractionListener: OnInteractionListener
): URLSpan(url) {

    override fun onClick(widget: View) {
        if (onInteractionListener.longClickCounter.get() == 0) {
            if (url.isSphinxUrl) {
                onInteractionListener.onClick(url)
            } else {
                super.onClick(widget)
            }
        } else {
            onInteractionListener.longClickCounter.set(0)
        }

    }

    abstract class OnInteractionListener(private val onLongClickListener: View.OnLongClickListener) : View.OnLongClickListener {
        val longClickCounter = AtomicInteger(0)

        /**
         * Called when a view has been clicked.
         *
         * @param url The url that was clicked.
         */
        abstract fun onClick(url: String?)

        override fun onLongClick(view: View): Boolean {
            longClickCounter.incrementAndGet()
            return onLongClickListener.onLongClick(view)
        }
    }
}
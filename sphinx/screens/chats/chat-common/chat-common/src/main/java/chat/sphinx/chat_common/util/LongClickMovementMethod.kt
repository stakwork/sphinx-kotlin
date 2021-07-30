package chat.sphinx.chat_common.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.text.Selection
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import chat.sphinx.chat_common.R

/**
 * Reference
 * - https://github.com/signalapp/Signal-Android/blob/8e93bf9075d3b997df3cc7e99ed1dd0f44897f19/app/src/main/java/org/thoughtcrime/securesms/util/LongClickMovementMethod.java
 */
class LongClickMovementMethod private constructor(context: Context) : LinkMovementMethod() {
    private val gestureDetector: GestureDetector
    private var widget: View? = null
    private var currentSpan: LongClickUrlSpan? = null
    override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
        val action = event.action
        if (action == MotionEvent.ACTION_UP ||
            action == MotionEvent.ACTION_DOWN
        ) {
            var x = event.x.toInt()
            var y = event.y.toInt()
            x -= widget.totalPaddingLeft
            y -= widget.totalPaddingTop
            x += widget.scrollX
            y += widget.scrollY
            val layout = widget.layout
            val line = layout.getLineForVertical(y)
            val off = layout.getOffsetForHorizontal(line, x.toFloat())
            val longClickSpan = buffer.getSpans(off, off, LongClickUrlSpan::class.java)
            if (longClickSpan.isNotEmpty()) {
                val aSingleSpan = longClickSpan[0]
                if (action == MotionEvent.ACTION_DOWN) {
                    Selection.setSelection(
                        buffer, buffer.getSpanStart(aSingleSpan),
                        buffer.getSpanEnd(aSingleSpan)
                    )
                    aSingleSpan.setHighlighted(
                        true,
                        ContextCompat.getColor(widget.context, R.color.blueTextAccent)
                    )
                } else {
                    Selection.removeSelection(buffer)
                    aSingleSpan.setHighlighted(false, Color.TRANSPARENT)
                }
                currentSpan = aSingleSpan
                this.widget = widget
                return gestureDetector.onTouchEvent(event)
            }
        } else if (action == MotionEvent.ACTION_CANCEL) {
            // Remove Selections.
            val spans = buffer.getSpans(
                Selection.getSelectionStart(buffer),
                Selection.getSelectionEnd(buffer), LongClickUrlSpan::class.java
            )
            for (aSpan in spans) {
                aSpan.setHighlighted(false, Color.TRANSPARENT)
            }
            Selection.removeSelection(buffer)
            return gestureDetector.onTouchEvent(event)
        }
        return super.onTouchEvent(widget, buffer, event)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var sInstance: LongClickMovementMethod? = null
        fun getInstance(context: Context): LongClickMovementMethod? {
            if (sInstance == null) {
                sInstance = LongClickMovementMethod(context.applicationContext)
            }
            return sInstance
        }
    }

    init {
        gestureDetector = GestureDetector(context, object : SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                if (currentSpan != null && widget != null) {
                    currentSpan!!.onLongClick(widget!!)
                    widget = null
                    currentSpan = null
                }
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (currentSpan != null && widget != null) {
                    currentSpan!!.onClick(widget!!)
                    widget = null
                    currentSpan = null
                }
                return true
            }
        })
    }
}
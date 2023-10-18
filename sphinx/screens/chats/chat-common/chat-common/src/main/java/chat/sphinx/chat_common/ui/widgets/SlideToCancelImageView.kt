package chat.sphinx.chat_common.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatImageView

class SlideToCancelImageView : AppCompatImageView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    var slideToCancelListener: SlideToCancelListener? = null

    private val gestureDetector = GestureDetector(context, object: GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            slideToCancelListener?.let { slideToCancelListener ->
                if (slideToCancelListener.isActive()) {
                    if (slideToCancelListener.thresholdX() > e2.rawX) {
                        slideToCancelListener.onSlideToCancel()
                    }

                }
            }

            return true
        }
    })

    init {
        gestureDetector.setIsLongpressEnabled(false)
    }

    interface SlideToCancelListener {
        fun isActive(): Boolean
        fun thresholdX(): Float

        fun onSlideToCancel()
        fun onInteractionComplete()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_UP && slideToCancelListener?.isActive() == true) {
            slideToCancelListener?.onInteractionComplete()
        }

        var result = event?.let { gestureDetector.onTouchEvent(it) }
        result = super.onTouchEvent(event) || result == true

        return result
    }
}
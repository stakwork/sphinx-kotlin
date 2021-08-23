package chat.sphinx.chat_common.ui

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

fun Float.rounded(): Float {
    return ((this*1000).toInt()/1000.0f)
}

class SphinxFullscreenImageView : AppCompatImageView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    var onSingleTapListener: OnSingleTapListener? = null

    private var scaleFactor = 1.0f;

    private val scaleGestureDetector = ScaleGestureDetector(context, object: ScaleGestureDetector.SimpleOnScaleGestureListener() {

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor = 1.0f.coerceAtLeast(scaleFactor * detector.scaleFactor)

            scaleX = scaleFactor.rounded()
            scaleY = scaleFactor.rounded()

            return true
        }
    })

    private val gestureDetector = GestureDetector(context, object: GestureDetector.SimpleOnGestureListener() {
        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent?,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            if (!scaleGestureDetector.isInProgress && drawable != null) {
                val scaledWidth = measuredWidth * scaleX

                val maximumTranslationX = (scaledWidth-measuredWidth)/2
                val minimumTranslationX = 0 - maximumTranslationX

                val measuredHeight = drawable.intrinsicHeight
                val scaledHeight = measuredHeight * scaleY

                val maximumTranslationY = (scaledHeight-measuredHeight)/2
                val minimumTranslationY = 0 - maximumTranslationY

                if (scaleX > 1.0f) {
                    // We are in zoom mood so we need to be moving the image around
                    translationX = (translationX - distanceX)
                        .coerceIn(minimumTranslationX, maximumTranslationX)
                    translationY = (translationY - distanceY)
                        .coerceIn(minimumTranslationY, maximumTranslationY)
                }

                return true
            }
            return false
        }

        /**
         * On Single tap toggle visibility of the header
         */
        override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
            if (!scaleGestureDetector.isInProgress) {
                onSingleTapListener?.onSingleTapConfirmed()
            }
            return true
        }

        /**
         * On Double Tap zoom in and out
         */
        override fun onDoubleTap(e: MotionEvent?): Boolean {
            scaleFactor = if (scaleFactor == 1.0f) {
                2.0f
            } else {
                // When going back to the normal scale we should fix the translations
                translationX = 0.0f
                translationY = 0.0f

                1.0f
            }

            scaleX = scaleFactor.rounded()
            scaleY = scaleFactor.rounded()
            return true
        }
    })

    interface OnSingleTapListener {
        fun onSingleTapConfirmed()
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        var result = scaleGestureDetector.onTouchEvent(event)
        result = gestureDetector.onTouchEvent(event) || result
        return super.onTouchEvent(event) || result
    }
}
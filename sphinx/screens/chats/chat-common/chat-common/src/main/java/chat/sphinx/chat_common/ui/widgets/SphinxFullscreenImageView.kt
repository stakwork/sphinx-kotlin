package chat.sphinx.chat_common.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.absoluteValue

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
    var onCloseViewHandler: OnCloseViewHandler? = null

    private var scaleFactor = 1.0f

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
            e1: MotionEvent,
            e2: MotionEvent,
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
                } else {
                    // Enable user to swipe to close the image...
                    translationY -= distanceY

                    if (translationY.absoluteValue > 300) {
                        onCloseViewHandler?.performClose()
                    }
                }

                return true
            }
            return false
        }

        /**
         * On Single tap toggle visibility of the header
         */
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            if (!scaleGestureDetector.isInProgress) {
                onSingleTapListener?.onSingleTapConfirmed()
            }
            return true
        }

        /**
         * On Double Tap zoom in and out
         */
        override fun onDoubleTap(e: MotionEvent): Boolean {
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

    abstract class OnCloseViewHandler {
        var isInProgress = false

        fun performClose() {
            isInProgress = true
            onCloseView()
        }

        protected abstract fun onCloseView()
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (onCloseViewHandler?.isInProgress == true) {
            // We don't handle any touch events when closing
            return true
        }
        var result = scaleGestureDetector.onTouchEvent(event)
        result = gestureDetector.onTouchEvent(event) || result

        if ( event.action == MotionEvent.ACTION_UP && scaleFactor == 1.0f) {
            if (translationY.absoluteValue > 0) {
                // When user stops scrolling the image
                animate()
                    .translationY(0f)
                    .setDuration(300L)
                    .start()
            }
        }
        return super.onTouchEvent(event) || result
    }

    fun resetInteractionProperties() {
        scaleX = 1.0f
        scaleY = 1.0f
        translationX = 0f
        translationY = 0f

        onCloseViewHandler?.isInProgress = false
    }
}
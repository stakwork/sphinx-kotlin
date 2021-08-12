package chat.sphinx.resources

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.PixelCopy
import android.view.View
import android.view.Window
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.FontRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat


@Suppress("NOTHING_TO_INLINE")
inline fun TextView.setTextColorExt(@ColorRes resId: Int): TextView {
    setTextColor(
        ContextCompat.getColor(context, resId)
    )
    return this
}

@Suppress("NOTHING_TO_INLINE")
inline fun TextView.setTextFont(@FontRes resId: Int): TextView {
    val typeface: Typeface? = ResourcesCompat.getFont(context, resId)
    setTypeface(typeface)
    return this
}

/**
 * Providing a Drawable [resId] will retrieve the drawable and
 * set it's tint to the random color, then set the view's background
 * to that drawable.
 *
 * Other wise it sets the view's background to the random color.
 *
 * Alternatively, you can pass a value for [color] which provides
 * flexibility if you want to use a specific random color throughout
 * a given screen.
 * */
@Suppress("NOTHING_TO_INLINE")
inline fun View.setBackgroundRandomColor(
    @DrawableRes resId: Int?,
    @ColorInt color: Int? = null
) {
    if (resId != null) {
        val drawable = ContextCompat.getDrawable(this.context, resId)
        drawable?.setTint(color ?: this.context.getRandomColor())
        this.background = drawable
    } else {
        this.setBackgroundColor(color ?: this.context.getRandomColor())
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun View.setInitialsColor(
    @ColorInt color: Int?,
    @DrawableRes resId: Int?,
) {
    if (resId != null) {
        val drawable = ContextCompat.getDrawable(this.context, resId)
        drawable?.setTint(color ?: this.context.getRandomColor())
        this.background = drawable
    } else {
        this.setBackgroundColor(color ?: this.context.getRandomColor())
    }
}

inline fun Bitmap.blur(context: Context, radius:Float = 10F): Bitmap?{
    val bitmap = copy(config,true)

    RenderScript.create(context).apply {
        val input = Allocation.createFromBitmap(this,this@blur)
        val output = Allocation.createFromBitmap(this,this@blur)

        ScriptIntrinsicBlur.create(this, Element.U8_4(this)).apply {
            setInput(input)

            setRadius(radius)
            forEach(output)

            output.copyTo(bitmap)
            destroy()
        }
    }
    return bitmap
}

// https://stackoverflow.com/a/58315279
@Suppress("NOTHING_TO_INLINE")
inline fun View.takeScreenshot(
    window: Window,
    crossinline bitmapCallback: (Bitmap) -> Unit,
    crossinline errorCallback: () -> Unit,
) {
    val width = measuredWidth
    val height = measuredHeight
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val location = IntArray(2)
        getLocationInWindow(location)
        PixelCopy.request(
            window,
            Rect(location[0], location[1], location[0] + width, location[1] + height),
            bitmap,
            {
                if (it == PixelCopy.SUCCESS) {
                    bitmapCallback.invoke(bitmap)
                } else {
                    errorCallback.invoke()
                }
            },
            Handler(Looper.getMainLooper())
        )
    } else {
        val canvas = Canvas(bitmap)
        val bgDrawable = background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }

        try {
            this.draw(canvas)
            canvas.setBitmap(null)
            bitmapCallback.invoke(bitmap)
        } catch (e: Exception) {
            errorCallback.invoke()
        }
    }
}

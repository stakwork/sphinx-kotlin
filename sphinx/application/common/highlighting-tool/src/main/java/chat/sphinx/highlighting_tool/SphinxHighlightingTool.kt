package chat.sphinx.highlighting_tool

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.text.style.TypefaceSpan
import android.widget.TextView
import androidx.annotation.IntRange
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import java.util.*


/**
 * LinkifyCompat brings in `Linkify` improvements for URLs and email addresses to older API
 * levels.
 */
@SuppressLint("RestrictedApi")
object SphinxHighlightingTool {
    /**
     * Scans the text of the provided TextView and turns all occurrences of
     * the link types indicated in the mask into clickable links.  If matches
     * are found the movement method for the TextView is set to
     * LinkMovementMethod.
     *
     * @param text TextView whose text is to be marked-up with links
     * @param mask Mask to define which kinds of links will be searched.
     *
     * @return True if at least one link is found and applied.
     */
    fun addHighlights(
        text: TextView,
        highlightedTexts: List<Pair<String, IntRange>>,
        resources: Resources,
        context: Context
    ) {

        if (highlightedTexts.isNotEmpty()) {
            val t = text.text
            if (t is Spannable) {
                for (highlightedText in highlightedTexts) {
                    ResourcesCompat.getFont(context, R.font.roboto_light)?.let { typeface ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            t.setSpan(
                                TypefaceSpan(typeface),
                                highlightedText.second.from.toInt(),
                                highlightedText.second.to.toInt(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                    }

                    t.setSpan(
                        BackgroundColorSpan(resources.getColor(R.color.highlightedTextBackground)),
                        highlightedText.second.from.toInt(),
                        highlightedText.second.to.toInt(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    text.setText(t, TextView.BufferType.SPANNABLE)
                }
            } else {
                val spannable: Spannable = SpannableString(text.text)

                for (highlightedText in highlightedTexts) {
                    ResourcesCompat.getFont(context, R.font.roboto_light)?.let { typeface ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            spannable.setSpan(
                                TypefaceSpan(typeface),
                                highlightedText.second.from.toInt(),
                                highlightedText.second.to.toInt(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                    }

                    spannable.setSpan(
                        BackgroundColorSpan(resources.getColor(R.color.highlightedTextBackground)),
                        highlightedText.second.from.toInt(),
                        highlightedText.second.to.toInt(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )

                    text.setText(spannable, TextView.BufferType.SPANNABLE)
                }
            }
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun String.highlightedTexts(): List<Pair<String, IntRange>> {
    val matcher = "`([^`]*)`".toRegex()
    val ranges = matcher.findAll(this).map{ it.range }.toList()

    if (ranges.isEmpty()) {
        return emptyList()
    }

    var adaptedText = this

    ranges.forEachIndexed { index, range ->
        val subtraction = index * 2

        val adaptedRange = IntRange(
            start = range.first - subtraction,
            endInclusive = range.last - subtraction
        )

        val rangeString = adaptedText.substring(adaptedRange.first, adaptedRange.last).replace("`","")

        adaptedText = adaptedText.replaceRange(adaptedRange, rangeString)
    }

    var matches: MutableList<Pair<String, IntRange>> = mutableListOf()

    ranges.forEachIndexed { index, range ->
        val subtraction = index * 2

        val adaptedRange = IntRange(
            from = (range.first - subtraction).toLong(),
            to = (range.last - subtraction - 1).toLong()
        )

        val rangeString = adaptedText.substring(adaptedRange.from.toInt(), adaptedRange.to.toInt())

        matches.add(
            Pair(rangeString, adaptedRange)
        )
    }

    return matches
}

@Suppress("NOTHING_TO_INLINE")
inline fun String.replacingHighlightedDelimiters(): String {
    val matcher = "`([^`]*)`".toRegex()
    val ranges = matcher.findAll(this).map{ it.range }.toList()

    if (ranges.isEmpty()) {
        return this
    }

    var adaptedText = this

    ranges.forEachIndexed { index, range ->
        val subtraction = index * 2

        val adaptedRange = IntRange(
            start = range.first - subtraction,
            endInclusive = range.last - subtraction
        )

        val rangeString = adaptedText.substring(adaptedRange.first, adaptedRange.last).replace("`","")

        adaptedText = adaptedText.replaceRange(adaptedRange, rangeString)
    }

    return adaptedText
}
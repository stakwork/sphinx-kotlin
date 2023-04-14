package chat.sphinx.chat_common.util

import android.annotation.SuppressLint
import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.text.util.Linkify
import android.text.util.Linkify.MatchFilter
import android.text.util.Linkify.TransformFilter
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.core.util.PatternsCompat
import chat.sphinx.chat_common.R
import chat.sphinx.wrapper_common.feed.FeedItemLink
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.VirtualLightningNodeAddress
import chat.sphinx.wrapper_common.tribe.TribeJoinLink
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern


/**
 * LinkifyCompat brings in `Linkify` improvements for URLs and email addresses to older API
 * levels.
 */
@SuppressLint("RestrictedApi")
object SphinxLinkify {

    /**
     * Bit field indicating that web URLs should be matched in methods that
     * take an options mask
     */
    const val WEB_URLS = 0x01

    /**
     * Bit field indicating that email addresses should be matched in methods
     * that take an options mask
     */
    const val EMAIL_ADDRESSES: Int = 0x02

    /**
     * Bit field indicating that phone numbers should be matched in methods that
     * take an options mask
     */
    const val PHONE_NUMBERS: Int = 0x04

    /**
     * Bit field indicating that [LightningNodePubKey] should be matched in methods that
     * take an options mask
     */
    const val LIGHTNING_NODE_PUBLIC_KEY: Int = 0x08

    /**
     * Bit field indicating that [VirtualLightningNodeAddress] should be matched in methods that
     * take an options mask
     */
    const val VIRTUAL_NODE_ADDRESS: Int = 0x16

    /**
     * Bit field indicating that Tribe Member Mention should be matched in methods that
     * take an options mask
     */
    const val MENTION: Int = 0x32

    /**
     * Bit field indicating that [FeedItemLink] should be matched in methods that
     * take an options mask
     */
    const val FEED_ITEM: Int = 0x64

    /**
     * Bit mask indicating that all available patterns should be matched in
     * methods that take an options mask
     *
     * **Note:** [.MAP_ADDRESSES] is deprecated.
     * Use [android.view.textclassifier.TextClassifier.generateLinks]
     * instead and avoid it even when targeting API levels where no alternative is available.
     */
    const val ALL: Int = WEB_URLS or EMAIL_ADDRESSES or PHONE_NUMBERS or LIGHTNING_NODE_PUBLIC_KEY or VIRTUAL_NODE_ADDRESS or MENTION or FEED_ITEM

    private val COMPARATOR: Comparator<LinkSpec> = object : Comparator<LinkSpec> {
        override fun compare(a: LinkSpec, b: LinkSpec): Int {
            if (a.start < b.start) {
                return -1
            }
            if (a.start > b.start) {
                return 1
            }
            if (a.end < b.end) {
                return 1
            }
            return if (a.end > b.end) {
                -1
            } else 0
        }
    }

    /**
     * Scans the text of the provided Spannable and turns all occurrences
     * of the link types indicated in the mask into clickable links.
     * If the mask is nonzero, it also removes any existing URLSpans
     * attached to the Spannable, to avoid problems if you call it
     * repeatedly on the same text.
     *
     * @param text Spannable whose text is to be marked-up with links
     * @param mask Mask to define which kinds of links will be searched.
     *
     * @return True if at least one link is found and applied.
     */
    private fun addLinks(
        text: Spannable,
        @LinkifyMask mask: Int,
        context: Context,
        onSphinxInteractionListener: SphinxUrlSpan.OnInteractionListener,
    ): Boolean {
        if (mask == 0) {
            return false
        }
        val old = text.getSpans(0, text.length, URLSpan::class.java)
        for (i in old.indices.reversed()) {
            text.removeSpan(old[i])
        }
        val links = ArrayList<LinkSpec>()
        if (mask and LIGHTNING_NODE_PUBLIC_KEY != 0) {
            gatherLinks(
                links, text, SphinxPatterns.LIGHTNING_NODE_PUBLIC_KEY, arrayOf(),
                null, null
            )
        }
        if (mask and VIRTUAL_NODE_ADDRESS != 0) {
            gatherLinks(
                links, text, SphinxPatterns.VIRTUAL_NODE_ADDRESS, arrayOf(),
                null, null
            )
        }
        if (mask and PHONE_NUMBERS != 0) {
            Linkify.addLinks(text, Linkify.PHONE_NUMBERS)
        }

        if (mask and FEED_ITEM != 0) {
            gatherLinks(
                links, text, SphinxPatterns.FEED_ITEM, arrayOf(),
                null, null
            )
        }

        if (mask and WEB_URLS != 0) {
            gatherLinks(
                links,
                text,
                SphinxPatterns.AUTOLINK_WEB_URL,
                arrayOf("http://", "https://", "rtsp://", "sphinx.chat://"),
                Linkify.sUrlMatchFilter,
                null
            )
        }
        if (mask and EMAIL_ADDRESSES != 0) {
            gatherLinks(
                links, text, PatternsCompat.AUTOLINK_EMAIL_ADDRESS, arrayOf("mailto:"),
                null, null
            )
        }
        if (mask and MENTION != 0) {
            gatherLinks(
                links, text, SphinxPatterns.MENTION, arrayOf(),
                null, null, false, context.getColor(R.color.primaryBlue)
            )
        }
        pruneOverlaps(links, text)
        if (links.size == 0) {
            return false
        }
        for (link in links) {
            if (link.frameworkAddedSpan == null) {
                applyLink(link, text, context, onSphinxInteractionListener)
            }
        }
        return true
    }

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
    fun addLinks(
        text: TextView,
        @LinkifyMask mask: Int,
        context: Context,
        onSphinxInteractionListener: SphinxUrlSpan.OnInteractionListener,
    ): Boolean {
        if (mask == 0) {
            return false
        }

        val t = text.text
        return if (t is Spannable) {
            if (addLinks(t, mask, context, onSphinxInteractionListener)) {
                addLinkMovementMethod(text)
                return true
            }
            false
        } else {
            val s = SpannableString.valueOf(t)
            if (addLinks(s, mask, context, onSphinxInteractionListener)) {
                addLinkMovementMethod(text)
                text.text = s
                return true
            }
            false
        }
    }

    private fun addLinkMovementMethod(t: TextView) {
        val m = t.movementMethod
        if (m !is LinkMovementMethod) {
            if (t.linksClickable) {
                t.movementMethod = LinkMovementMethod.getInstance()
            }
        }
    }

    private fun makeUrl(
        url: String, prefixes: Array<String?>,
        matcher: Matcher, filter: TransformFilter?
    ): String {
        var localUrl = url
        if (filter != null) {
            localUrl = filter.transformUrl(matcher, localUrl)
        }
        var hasPrefix = false
        for (i in prefixes.indices) {
            if (localUrl.regionMatches(0, prefixes[i]!!, 0, prefixes[i]!!.length, ignoreCase = true)) {
                hasPrefix = true

                // Fix capitalization if necessary
                if (!localUrl.regionMatches(
                        0,
                        prefixes[i]!!, 0, prefixes[i]!!.length, ignoreCase = false
                    )
                ) {
                    localUrl = prefixes[i].toString() + localUrl.substring(prefixes[i]!!.length)
                }
                break
            }
        }
        if (!hasPrefix && prefixes.size > 0) {
            localUrl = prefixes[0].toString() + localUrl
        }
        return localUrl
    }

    private fun gatherLinks(
        links: ArrayList<LinkSpec>,
        s: Spannable, pattern: Pattern, schemes: Array<String?>,
        matchFilter: MatchFilter?, transformFilter: TransformFilter?,
        underline: Boolean = true,
        @ColorInt color: Int? = null,
    ) {
        val m = pattern.matcher(s)
        while (m.find()) {
            val start = m.start()
            val end = m.end()
            if (matchFilter == null || matchFilter.acceptMatch(s, start, end)) {
                val spec = LinkSpec()
                val url = makeUrl(m.group(0), schemes, m, transformFilter)
                spec.url = url
                spec.start = start
                spec.end = end
                spec.underline = underline
                spec.color = color
                links.add(spec)
            }
        }
    }

    private fun applyLink(
        linkSpec: LinkSpec,
        text: Spannable,
        context: Context,
        onSphinxInteractionListener: SphinxUrlSpan.OnInteractionListener,
    ) {
        val span = SphinxUrlSpan(linkSpec.url, linkSpec.underline, linkSpec.color ?: context.getColor(R.color.primaryBlue), onSphinxInteractionListener)
        text.setSpan(span, linkSpec.start, linkSpec.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun pruneOverlaps(links: ArrayList<LinkSpec>, text: Spannable) {
        // Append spans added by framework
        val urlSpans = text.getSpans(0, text.length, URLSpan::class.java)
        for (i in urlSpans.indices) {
            val spec = LinkSpec()
            spec.frameworkAddedSpan = urlSpans[i]
            spec.start = text.getSpanStart(urlSpans[i])
            spec.end = text.getSpanEnd(urlSpans[i])
            links.add(spec)
        }
        Collections.sort(links, COMPARATOR)
        var len = links.size
        var i = 0
        while (i < len - 1) {
            val a = links[i]
            val b = links[i + 1]
            var remove = -1
            if (a.start <= b.start && a.end > b.start) {
                if (b.end <= a.end) {
                    remove = i + 1
                } else if (a.end - a.start > b.end - b.start) {
                    remove = i + 1
                } else if (a.end - a.start < b.end - b.start) {
                    remove = i
                }
                if (remove != -1) {
                    val span = links[remove].frameworkAddedSpan
                    if (span != null) {
                        text.removeSpan(span)
                    }
                    links.removeAt(remove)
                    len--
                    continue
                }
            }
            i++
        }
    }

    /** @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(
        flag = true,
        value = [WEB_URLS, EMAIL_ADDRESSES, PHONE_NUMBERS, LIGHTNING_NODE_PUBLIC_KEY, VIRTUAL_NODE_ADDRESS, MENTION, ALL]
    )
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class LinkifyMask
    private class LinkSpec internal constructor() {
        var frameworkAddedSpan: URLSpan? = null
        var url: String? = null
        var start = 0
        var end = 0
        var underline = true
        @ColorInt var color: Int? = null
    }

    object SphinxPatterns {
        val AUTOLINK_WEB_URL: Pattern = Pattern.compile(
            "(${TribeJoinLink.REGEX}|${PatternsCompat.AUTOLINK_WEB_URL.pattern()})"
        )

        val LIGHTNING_NODE_PUBLIC_KEY: Pattern = Pattern.compile(
            LightningNodePubKey.REGEX
        )

        val VIRTUAL_NODE_ADDRESS: Pattern = Pattern.compile(
            VirtualLightningNodeAddress.REGEX
        )

        val LINK_PREVIEWS: Pattern = Pattern.compile(
            "(${TribeJoinLink.REGEX}|${FeedItemLink.REGEX}|${PatternsCompat.AUTOLINK_WEB_URL.pattern()}|${VirtualLightningNodeAddress.REGEX}|${LightningNodePubKey.REGEX})"
        )
            
        val COPYABLE_LINKS: Pattern = Pattern.compile(
            "(${TribeJoinLink.REGEX}|${PatternsCompat.AUTOLINK_WEB_URL.pattern()}|${VirtualLightningNodeAddress.REGEX}|${LightningNodePubKey.REGEX})"
        )

        val MENTION: Pattern = Pattern.compile(
            "\\B@[^\\s]+"
        )

        val FEED_ITEM: Pattern = Pattern.compile(
            FeedItemLink.REGEX
        )
    }
}
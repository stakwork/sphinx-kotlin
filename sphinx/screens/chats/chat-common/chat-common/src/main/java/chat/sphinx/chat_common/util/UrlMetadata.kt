package chat.sphinx.chat_common.util

import org.jsoup.nodes.Document

@Suppress("NOTHING_TO_INLINE")
internal inline fun Document.getTitle(): String? {
    title().let {
        if (it.isNotEmpty()) {
            return it
        }
    }
    head().selectFirst("meta[property=og:title]")?.let { element ->
        element.attr("content").let {
            if (it.isNotEmpty()) {
                return it
            }
        }
    }
    return null
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun Document.getDescription(): String? {
    head().selectFirst("meta[property=description]")?.let { element ->
        element.attr("content").let {
            if (it.isNotEmpty()) {
                return it
            }
        }
    }
    head().selectFirst("meta[property=og:description]")?.let { element ->
        element.attr("content").let {
            if (it.isNotEmpty()) {
                return it
            }
        }
    }
    return null
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun Document.getImageUrl(): String? {
    head().selectFirst("meta[property=og:image:secure_url]")?.let { element ->
        element.attr("content").let {
            if (it.isNotEmpty()) {
                return it
            }
        }
    }
    head().selectFirst("meta[property=og:image:url]")?.let { element ->
        element.attr("content").let {
            if (it.isNotEmpty()) {
                return it
            }
        }
    }
    head().selectFirst("meta[property=og:image]")?.let { element ->
        element.attr("content").let {
            if (it.isNotEmpty()) {
                return it
            }
        }
    }
    return null
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun Document.getFavIconUrl(): String? {
    head().selectFirst("link[rel=icon]")?.let { element ->
        element.attr("content").let {
            if (it.isNotEmpty()) {
                return it
            }
        }
    }
    head().selectFirst("link[rel=shortcut icon]")?.let { element ->
        element.attr("content").let {
            if (it.isNotEmpty()) {
                return it
            }
        }
    }
    return null
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun Document.canonicalUrl(url: String?): String? {
    return url?.let {
        return if (it.startsWith("http")) {
            it
        } else {
            "${baseUri()}$it"
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun Document.toUrlMetadata(): UrlMetadata? {
    getTitle()?.let { title ->
        if (title.isNotEmpty()) {
            return UrlMetadata(
                title,
                getDescription(),
                getImageUrl(),
                getFavIconUrl()
            )
        }
    }
    return null
}



class UrlMetadata(
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val favIconUrl: String?
)
package chat.sphinx.chat_common.util

import org.jsoup.nodes.Document

@Suppress("NOTHING_TO_INLINE")
internal inline fun Document.getTitle(): String? {
    title().let {
        if (it.isNotEmpty()) {
            return it
        }
    }
    head().selectFirst("meta[property=og:title]")?.let {
        return it.attr("content")
    }
    return null
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun Document.getDescription(): String? {
    head().selectFirst("meta[property=description]")?.let {
        return it.attr("content")
    }
    head().selectFirst("meta[property=og:description]")?.let {
        return it.attr("content")
    }
    return null
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun Document.getImageUrl(): String? {
    // TODO: Rebase the url...
    head().selectFirst("meta[property=og:image:secure_url]")?.let {
        return it.attr("content")
    }
    head().selectFirst("meta[property=og:image:url]")?.let {
        return it.attr("content")
    }
    head().selectFirst("meta[property=og:image]")?.let {
        return it.attr("content")
    }
    return null
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun Document.getFavIconUrl(): String? {
    head().selectFirst("link[rel=icon]")?.let {
        // TODO: Rebase the URL...
        return it.attr("href")
    }
    return null
}


@Suppress("NOTHING_TO_INLINE")
internal inline fun Document.toUrlMetadata(): UrlMetadata? {
    getTitle()?.let { title ->
        return UrlMetadata(
            title,
            getDescription(),
            getImageUrl(),
            getFavIconUrl()
        )
    }
    return null
}



class UrlMetadata(
    val title: String,
    val description: String?,
    val imageUrl: String?,
    val favIconUrl: String?
)
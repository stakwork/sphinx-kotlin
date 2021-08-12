package chat.sphinx.concept_link_preview.model

@Suppress("NOTHING_TO_INLINE")
inline fun String.toPreviewDescriptionOrNull(): PreviewDescription? =
    try {
        PreviewDescription(this)
    } catch (e: IllegalArgumentException) {
        null
    }

@JvmInline
value class PreviewDescription(val value: String) {
    init {
        require(value.isNotEmpty()) {
            "PreviewDescription cannot be empty"
        }
    }
}

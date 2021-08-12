package chat.sphinx.wrapper_message_media

inline val MediaType.isSphinxText: Boolean
    get() = this is MediaType.Text

inline val MediaType.isAudio: Boolean
    get() = this is MediaType.Audio

inline val MediaType.isImage: Boolean
    get() = this is MediaType.Image

inline val MediaType.isPdf: Boolean
    get() = this is MediaType.Pdf

inline val MediaType.isVideo: Boolean
    get() = this is MediaType.Video

inline val MediaType.isUnknown: Boolean
    get() = this is MediaType.Unknown

@Suppress("NOTHING_TO_INLINE")
inline fun String.toMediaType(): MediaType =
    when {
        this == MediaType.SPHINX_TEXT -> {
            MediaType.Text
        }
        contains(MediaType.AUDIO, ignoreCase = true) -> {
            MediaType.Audio(this)
        }
        contains(MediaType.IMAGE, ignoreCase = true) -> {
            MediaType.Image(this)
        }
        contains(MediaType.PDF, ignoreCase = true) -> {
            MediaType.Pdf(this)
        }
        contains(MediaType.VIDEO, ignoreCase = true) -> {
            MediaType.Video(this)
        }
        else -> {
            MediaType.Unknown(this)
        }
    }

sealed class MediaType {

    companion object {
        const val AUDIO = "audio"
        const val IMAGE = "image"
        const val PDF = "pdf"
        const val SPHINX_TEXT = "sphinx/text"
        const val VIDEO = "video"
    }

    abstract val value: String

    data class Audio(override val value: String): MediaType()

    data class Image(override val value: String): MediaType() {
        val isGif: Boolean
            get() = value.contains("gif", ignoreCase = true)
    }

    data class Pdf(override val value: String): MediaType()

    object Text: MediaType() {
        override val value: String
            get() = SPHINX_TEXT
    }

    data class Video(override val value: String): MediaType()

    data class Unknown(override val value: String): MediaType()
}

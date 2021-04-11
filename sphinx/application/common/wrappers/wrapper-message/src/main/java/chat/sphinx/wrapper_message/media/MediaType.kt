package chat.sphinx.wrapper_message.media

@Suppress("NOTHING_TO_INLINE")
inline fun String.toMediaType(): MediaType =
    when {
        this == MediaType.SPHINX_TEXT -> {
            MediaType.SphinxText
        }
        this.contains(MediaType.AUDIO, ignoreCase = true) -> {
            MediaType.Audio(this)
        }
        this.contains(MediaType.GIF, ignoreCase = true) -> {
            MediaType.Gif(this)
        }
        this.contains(MediaType.IMAGE, ignoreCase = true) -> {
            MediaType.Image(this)
        }
        this.contains(MediaType.PDF, ignoreCase = true) -> {
            MediaType.Pdf(this)
        }
        this.contains(MediaType.VIDEO, ignoreCase = true) -> {
            MediaType.Video(this)
        }
        else -> {
            MediaType.Unknown(this)
        }
    }

sealed class MediaType {

    companion object {
        const val SPHINX_TEXT = "sphinx/text"
        const val AUDIO = "audio"
        const val GIF = "image/gif"
        const val IMAGE = "image"
        const val PDF = "pdf"
        const val VIDEO = "video"
    }

    abstract val value: String

    object SphinxText: MediaType() {
        override val value: String
            get() = SPHINX_TEXT
    }

    class Audio(override val value: String): MediaType()

    class Gif(override val value: String): MediaType()

    class Image(override val value: String): MediaType()

    class Pdf(override val value: String): MediaType()

    class Video(override val value: String): MediaType()

    class Unknown(override val value: String): MediaType()
}

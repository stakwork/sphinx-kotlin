package chat.sphinx.wrapper_common

@Suppress("NOTHING_TO_INLINE")
inline fun PreviewsEnabled.isTrue(): Boolean =
    this is PreviewsEnabled.True

@Suppress("NOTHING_TO_INLINE")
inline fun Int.toPreviewsEnabled(): PreviewsEnabled =
    when (this) {
        PreviewsEnabled.ENABLED -> {
            PreviewsEnabled.True
        }
        else -> {
            PreviewsEnabled.False
        }
    }

@Suppress("NOTHING_TO_INLINE")
inline fun Boolean.toPreviewsEnabled(): PreviewsEnabled =
    if (this) PreviewsEnabled.True else PreviewsEnabled.False

sealed class PreviewsEnabled {

    companion object {
        const val ENABLED = 1
        const val DISABLED = 0

        const val LINK_PREVIEWS_SHARED_PREFERENCES = "general_settings"
        const val LINK_PREVIEWS_ENABLED_KEY = "link-previews-enabled"
    }

    abstract val value: Int

    object True: PreviewsEnabled() {
        override val value: Int
            get() = ENABLED
    }

    object False: PreviewsEnabled() {
        override val value: Int
            get() = DISABLED
    }
}

package chat.sphinx.example.delete_media_detail.viewstate

import chat.sphinx.example.delete_media_detail.model.MediaItem


sealed class MediaItemHolderViewState(
    val mediaItem: MediaItem? = null
) {
    object Loader : MediaItemHolderViewState()

    class Section(
        mediaItem: MediaItem
    ) : MediaItemHolderViewState(
        mediaItem
    )
}
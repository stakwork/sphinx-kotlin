package chat.sphinx.example.delete_media.viewstate

import chat.sphinx.example.delete_media.model.MediaSection

sealed class SectionHolderViewState(
    val mediaSection: MediaSection? = null
) {
    object Loader : SectionHolderViewState()

    class Section(
        mediaSection: MediaSection
    ) : SectionHolderViewState(
        mediaSection
    )
}
package chat.sphinx.example.delete_media.viewstate

import chat.sphinx.example.delete_media.model.MediaSection
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class DeleteMediaViewState: ViewState<DeleteMediaViewState>() {
    object Loading : DeleteMediaViewState()

    class SectionList(
        val section: List<MediaSection>,
        val totalSizeAllSections: String
    ) : DeleteMediaViewState()



}

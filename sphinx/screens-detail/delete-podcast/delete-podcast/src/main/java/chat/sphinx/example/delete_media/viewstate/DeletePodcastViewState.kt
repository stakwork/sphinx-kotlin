package chat.sphinx.example.delete_media.viewstate

import chat.sphinx.example.delete_media.model.PodcastToDelete
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class DeletePodcastViewState: ViewState<DeletePodcastViewState>() {
    object Loading : DeletePodcastViewState()

    class SectionList(
        val section: List<PodcastToDelete>,
        val totalSizeAllSections: String?
    ) : DeletePodcastViewState()

}

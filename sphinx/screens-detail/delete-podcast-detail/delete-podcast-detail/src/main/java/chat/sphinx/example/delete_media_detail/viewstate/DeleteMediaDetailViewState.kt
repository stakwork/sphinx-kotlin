package chat.sphinx.example.delete_media_detail.viewstate

import chat.sphinx.example.delete_media_detail.model.PodcastDetailToDelete
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class DeleteMediaDetailViewState : ViewState<DeleteMediaDetailViewState>() {
    object Idle : DeleteMediaDetailViewState()

    class EpisodeList(
        val feedName: String,
        val totalSize: String,
        val episodes: List<PodcastDetailToDelete>
    ): DeleteMediaDetailViewState()
}

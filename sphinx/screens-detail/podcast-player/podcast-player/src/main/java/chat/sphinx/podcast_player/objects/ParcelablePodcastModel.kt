package chat.sphinx.podcast_player.objects

import android.os.Parcelable
import chat.sphinx.concept_network_query_chat.model.PodcastModelDto
import chat.sphinx.wrapper_feed.*
import chat.sphinx.wrapper_podcast.PodcastModel
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParcelablePodcastModel(
    val type: String,
    val suggested: Double,
    val podcastId: String,
): Parcelable

fun PodcastModel.toParcelablePodcastModel(): ParcelablePodcastModel {
    return ParcelablePodcastModel(
        this.type.value,
        this.suggested.value,
        this.podcastId.value
    )
}

fun ParcelablePodcastModel.toPodcastModel(): PodcastModel {
    return PodcastModel(
        this.type.toFeedModelType() ?: FeedModelType("null"),
        this.suggested.toFeedModelSuggested() ?: FeedModelSuggested(0.0),
        this.podcastId.toFeedId() ?: FeedId("null")
    )
}
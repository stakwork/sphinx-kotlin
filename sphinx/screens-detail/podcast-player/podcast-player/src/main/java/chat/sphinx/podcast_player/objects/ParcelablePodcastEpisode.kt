package chat.sphinx.podcast_player.objects

import android.os.Parcelable
import chat.sphinx.concept_network_query_chat.model.PodcastEpisodeDto
import chat.sphinx.wrapper_podcast.PodcastEpisode
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class ParcelablePodcastEpisode(
    val id: Long,
    val title: String,
    val description: String,
    val image: String,
    val link: String,
    val enclosureUrl: String,
): Parcelable

fun PodcastEpisode.toParcelablePodcastEpisode(): ParcelablePodcastEpisode {
    return ParcelablePodcastEpisode(this.id, this.title ,this.description, this.image, this.link, this.enclosureUrl)
}

fun ParcelablePodcastEpisode.toPodcastEpisode(): PodcastEpisode {
    return PodcastEpisode(this.id, this.title ,this.description, this.image, this.link, this.enclosureUrl)
}
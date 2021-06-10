package chat.sphinx.podcast_player.objects

import android.os.Parcelable
import chat.sphinx.concept_network_query_chat.model.PodcastEpisodeDto
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
data class PodcastEpisode(
    val id: Long,
    val title: String,
    val description: String,
    val image: String,
    val link: String,
    val enclosureUrl: String,
): Parcelable {

    var playing: Boolean = false
    var downloaded: Boolean = false
}

fun PodcastEpisodeDto.toPodcastEpisode(): PodcastEpisode {
    return PodcastEpisode(this.id, this.title ,this.description, this.image, this.link, this.enclosureUrl)
}
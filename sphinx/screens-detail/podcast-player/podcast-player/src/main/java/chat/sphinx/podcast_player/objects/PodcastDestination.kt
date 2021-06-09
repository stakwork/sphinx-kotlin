package chat.sphinx.podcast_player.objects

import android.os.Parcelable
import chat.sphinx.concept_network_query_chat.model.PodcastDestinationDto
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
@JsonClass(generateAdapter = true)
data class PodcastDestination(
    val split: Long,
    val address: String,
    val type: String,
): Parcelable {
}

fun PodcastDestinationDto.toPodcastDestination(): PodcastDestination {
    return PodcastDestination(this.split, this.address, this.type)
}
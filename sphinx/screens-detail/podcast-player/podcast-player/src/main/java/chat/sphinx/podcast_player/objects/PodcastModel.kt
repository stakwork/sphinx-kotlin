package chat.sphinx.podcast_player.objects

import android.os.Parcelable
import chat.sphinx.concept_network_query_chat.model.PodcastModelDto
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize

@Parcelize
data class PodcastModel(
    val type: String,
    val suggested: Double,
): Parcelable {
}

fun PodcastModelDto.toPodcastModel(): PodcastModel {
    return PodcastModel(this.type, this.suggested)
}
package chat.sphinx.podcast_player.objects

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Parcelable
import chat.sphinx.concept_network_query_chat.model.PodcastDto
import chat.sphinx.wrapper_chat.ChatMetaData
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class Podcast(
    val id: Long,
    val title: String,
    val description: String,
    val author: String,
    val image: String,
    val value: PodcastValue,
    val episodes: List<PodcastEpisode>,
): Parcelable {

    @IgnoredOnParcel
    var episodeId: Long? = null

    @IgnoredOnParcel
    var timeSeconds: Int? = null

    @IgnoredOnParcel
    var speed: Double? = null

    fun setMetaData(metaData: ChatMetaData) {
        this.episodeId = metaData.itemId.value
        this.timeSeconds = metaData.timeSeconds
        this.speed = metaData.speed
    }

    fun getCurrentEpisode(): PodcastEpisode {
        episodeId?.let { episodeId ->
            for (episode in episodes) {
                if (episode.id == episodeId) {
                    return episode
                }
            }
        }
        return episodes[0]
    }

    fun getEpisodeDuration(episode: PodcastEpisode? = null): Long {
        val currentEpisode = episode ?: getCurrentEpisode()
        val uri = Uri.parse(currentEpisode.enclosureUrl)
        return uri.getMediaDuration()
    }

    fun getPlayingProgress(): Int {
        var currentTime = currentTime.toLong()
        val currentEpisode = getCurrentEpisode()
        val uri = Uri.parse(currentEpisode.enclosureUrl)
        val duration = uri.getMediaDuration()
        val progress = (currentTime * 100) / duration
        return progress.toInt()
    }

    val episodesCount: Int
        get() = episodes.count()

    val currentTime: Int
        get() = timeSeconds ?: 0
}

fun PodcastDto.toPodcast(): Podcast {
    val podcastEpisodes = mutableListOf<PodcastEpisode>()

    for (episode in this.episodes) {
        podcastEpisodes.add(episode.toPodcastEpisode())
    }

    return Podcast(this.id, this.title,this.description,this.author,this.image, this.value.toPodcastValue(), podcastEpisodes)
}

fun Uri.getMediaDuration(): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(this.toString())
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()
        duration?.toLongOrNull() ?: 0
    } catch (exception: Exception) {
        0
    }
}
package chat.sphinx.podcast_player.objects

import android.R.array
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Parcelable
import chat.sphinx.concept_network_query_chat.model.PodcastDto
import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_common.ItemId
import chat.sphinx.wrapper_common.lightning.Sat
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

    //MetaData
    @IgnoredOnParcel
    var episodeId: Long? = null

    @IgnoredOnParcel
    var timeSeconds: Int? = null

    @IgnoredOnParcel
    var speed: Double = 1.0

    @IgnoredOnParcel
    var satsPerMinute: Long? = null

    //Duration
    @IgnoredOnParcel
    var episodeDuration: Long? = null

    val episodesCount: Int
        get() = episodes.count()

    val currentTime: Int
        get() = timeSeconds ?: 0

    fun setMetaData(metaData: ChatMetaData) {
        this.episodeId = metaData.itemId.value
        this.timeSeconds = metaData.timeSeconds
        this.speed = metaData.speed
        this.satsPerMinute = metaData.satsPerMinute.value
    }

    fun getMetaData(): ChatMetaData {
        val episodeId = ItemId(this.episodeId ?: 0)
        val satsPerMinute = Sat(this.satsPerMinute ?: 0)
        val timeSeconds = this.timeSeconds ?: 0
        val speed = this.speed

        return ChatMetaData(episodeId, satsPerMinute, timeSeconds, speed)
    }

    fun didStartPlayingEpisode(episode: PodcastEpisode, time: Int) {
        episode.playing = true

        val didChangeEpisode = this.episodeId != episode.id
        this.episodeId = episode.id
        this.timeSeconds = time

        getCurrentEpisodeDuration(didChangeEpisode)
    }

    private fun didEndPlayingEpisode(episode: PodcastEpisode, nextEpisode: PodcastEpisode?) {
        episode.playing = false

        this.episodeId = nextEpisode?.id ?: episodes[0].id
        this.timeSeconds = 0

        getCurrentEpisodeDuration(true)
    }

    fun didStopPlayingEpisode(episode: PodcastEpisode) {
        episode.playing = false
    }

    fun didSeekTo(time: Int) {
        this.timeSeconds = time
    }

    fun getCurrentEpisode(): PodcastEpisode {
        episodeId?.let { episodeId ->
            return getEpisodeWithId(episodeId) ?: episodes[0]

        }
        return episodes[0]
    }

    private fun getEpisodeWithId(id: Long): PodcastEpisode? {
        for (episode in episodes) {
            if (episode.id == id) {
                return episode
            }
        }
        return null
    }

    private fun getNextEpisode(id: Long): PodcastEpisode? {
        for (i in episodes.indices) {
            if (episodes[i].id == id) {
                return episodes[i+1]
            }
        }
        return null
    }

    fun getCurrentEpisodeDuration(didChangeEpisode: Boolean = false): Long {
        if (episodeDuration == null || didChangeEpisode) {
            val currentEpisode = getCurrentEpisode()
            val uri = Uri.parse(currentEpisode.enclosureUrl)
            episodeDuration = uri.getMediaDuration()
        }

        return episodeDuration ?: 1
    }

    fun getPlayingProgress(): Int {
        var currentTime = currentTime.toLong()
        val duration = getCurrentEpisodeDuration()
        val progress = (currentTime * 100) / duration
        return progress.toInt()
    }

    //MediaService update
    fun playingEpisodeUpdate(episodeId: Long, time: Int) {
        val episode = getEpisodeWithId(episodeId)

        episode?.let { episode ->
            episode.playing = true

            this.episodeId = episode.id
            this.timeSeconds = time
        }
    }

    fun pauseEpisodeUpdate(episodeId: Long) {
        val episode = getEpisodeWithId(episodeId)

        episode?.let { episode ->
            didStopPlayingEpisode(episode)
        }
    }

    fun endEpisodeUpdate(episodeId: Long) {
        getEpisodeWithId(episodeId)?.let { episode ->
            val nextEpisode = getEpisodeWithId(episodeId)

            didEndPlayingEpisode(episode, nextEpisode)
        }
    }
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

fun PodcastDto.toPodcast(): Podcast {
    val podcastEpisodes = mutableListOf<PodcastEpisode>()

    for (episode in this.episodes) {
        podcastEpisodes.add(episode.toPodcastEpisode())
    }

    return Podcast(this.id, this.title,this.description,this.author,this.image, this.value.toPodcastValue(), podcastEpisodes)
}
package chat.sphinx.podcast_player.objects

import android.R.array
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Parcelable
import chat.sphinx.concept_network_query_chat.model.PodcastDto
import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_common.ItemId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.toSat
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize


@Parcelize
class Podcast(
    val id: Long,
    val title: String,
    val description: String,
    val author: String,
    val image: String,
    val value: PodcastValue,
    val episodes: List<PodcastEpisode>,
): Parcelable {

    //MetaData
    @Volatile
    @IgnoredOnParcel
    var episodeId: Long? = null

    @Volatile
    @IgnoredOnParcel
    var timeSeconds: Int? = null

    @Volatile
    @IgnoredOnParcel
    var speed: Double = 1.0

    @Volatile
    @IgnoredOnParcel
    var satsPerMinute: Long = 0

    //Duration
    @Volatile
    @IgnoredOnParcel
    var episodeDuration: Long? = null

    //Current Episode
    @Volatile
    @IgnoredOnParcel
    var playingEpisode: PodcastEpisode? = null


    val episodesCount: Int
        get() = episodes.count()

    val currentTime: Int
        get() = timeSeconds ?: 0

    val isPlaying: Boolean
        get() = playingEpisode?.playing ?: false

    fun setMetaData(metaData: ChatMetaData) {
        episodeId = metaData.itemId.value
        timeSeconds = metaData.timeSeconds
        speed = metaData.speed
        satsPerMinute = metaData.satsPerMinute.value

        playingEpisode = getEpisodeWithId(metaData.itemId.value)
    }

    fun getMetaData(): ChatMetaData =
        ChatMetaData(
            ItemId(episodeId ?: 0),
            satsPerMinute.toSat() ?: Sat(0),
            timeSeconds ?: 0,
            speed,
        )

    fun getCurrentEpisode(): PodcastEpisode {
        playingEpisode?.let { episode ->
            return episode
        } ?: run {
            return episodes[0]
        }
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

            if (playingEpisode == null) {
                playingEpisode = getCurrentEpisode()
            }

            playingEpisode?.let { episode ->
                val uri = Uri.parse(episode.enclosureUrl)
                episodeDuration = uri.getMediaDuration()
            }
        }

        return episodeDuration ?: 1
    }

    @Throws(ArithmeticException::class)
    fun getPlayingProgress(): Int {
        val progress = (currentTime.toLong() * 100) / getCurrentEpisodeDuration()
        return progress.toInt()
    }

    //User actions
    fun didStartPlayingEpisode(episode: PodcastEpisode, time: Int) {
        val didChangeEpisode = this.episodeId != episode.id

        if (didChangeEpisode) {
            this.playingEpisode?.playing = false

            this.playingEpisode = getEpisodeWithId(episode.id)
            this.playingEpisode?.playing = true

            this.episodeId = episode.id
        }
        this.timeSeconds = time

        getCurrentEpisodeDuration(didChangeEpisode)
    }

    private fun didEndPlayingEpisode(episode: PodcastEpisode, nextEpisode: PodcastEpisode?) {
        episode.playing = false

        val nextEpisodeId = nextEpisode?.id ?: episodes[0].id
        this.playingEpisode = getEpisodeWithId(nextEpisodeId)
        this.episodeId = nextEpisodeId

        this.timeSeconds = 0

        getCurrentEpisodeDuration(true)
    }

    fun didPausePlayingEpisode(episode: PodcastEpisode) {
        episode.playing = false
    }

    fun didSeekTo(time: Int) {
        this.timeSeconds = time
    }

    //MediaService update
    fun playingEpisodeUpdate(episodeId: Long, time: Int) {
        if (episodeId != playingEpisode?.id) {
            this.playingEpisode?.playing = false

            this.episodeDuration = null
            this.playingEpisode = getEpisodeWithId(episodeId)
        }

        playingEpisode?.let { nnEpisode ->
            nnEpisode.playing = true

            this.episodeId = nnEpisode.id
            this.timeSeconds = time
        }
    }

    fun pauseEpisodeUpdate() {
        playingEpisode?.let { nnEpisode ->
            didPausePlayingEpisode(nnEpisode)
        }
    }

    fun endEpisodeUpdate(episodeId: Long) {
        playingEpisode?.let { episode ->
            val nextEpisode = getNextEpisode(episodeId)
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
    val podcastEpisodes: MutableList<PodcastEpisode> = ArrayList(episodes.size)

    for (episode in episodes) {
        podcastEpisodes.add(episode.toPodcastEpisode())
    }

    return Podcast(id, title, description, author, image, value.toPodcastValue(), podcastEpisodes)
}
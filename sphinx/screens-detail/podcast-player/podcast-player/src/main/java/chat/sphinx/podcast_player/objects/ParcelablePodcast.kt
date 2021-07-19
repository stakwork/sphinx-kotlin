package chat.sphinx.podcast_player.objects

import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import chat.sphinx.concept_network_query_chat.model.PodcastDto
import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_common.ItemId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.toSat
import chat.sphinx.wrapper_podcast.Podcast
import chat.sphinx.wrapper_podcast.PodcastEpisode
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlin.math.roundToInt


@Parcelize
class ParcelablePodcast(
    val id: Long,
    val title: String,
    val description: String,
    val author: String,
    val image: String,
    val value: ParcelablePodcastValue,
    val episodes: List<ParcelablePodcastEpisode>,
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
    var playingEpisode: ParcelablePodcastEpisode? = null


    val episodesCount: Int
        get() = episodes.count()

    val currentTime: Int
        get() = timeSeconds ?: 0

    val isPlaying: Boolean
        get() = playingEpisode?.playing ?: false

    fun getEpisodesListCopy(): ArrayList<ParcelablePodcastEpisode> {
        var episodesList = ArrayList<ParcelablePodcastEpisode>()

        for (episode in this.episodes) {
            val episodeCopy = episode.copy()
            episodeCopy.playing = episode.playing
            episodeCopy.downloaded = episode.downloaded

            episodesList.add(episodeCopy)
        }
        return episodesList
    }

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

    fun getSpeedString(): String {
        if (speed.roundToInt().toDouble() == speed) {
            return "${speed.toInt()}x"
        }
        return "${speed}x"
    }

    fun getCurrentEpisode(): ParcelablePodcastEpisode {
        playingEpisode?.let { episode ->
            return episode
        } ?: run {
            return episodes[0]
        }
    }

    private fun getEpisodeWithId(id: Long): ParcelablePodcastEpisode? {
        for (episode in episodes) {
            if (episode.id == id) {
                return episode
            }
        }
        return episodes[0]
    }

    private fun getNextEpisode(id: Long): ParcelablePodcastEpisode {
        for (i in episodes.indices) {
            if (episodes[i].id == id && i-1 >= 0) {
                return episodes[i-1]
            }
        }
        return episodes[0]
    }

    fun getCurrentEpisodeDuration(): Long {
        if (episodeDuration == null) {

            if (playingEpisode == null) {
                playingEpisode = getCurrentEpisode()
            }

            playingEpisode?.let { episode ->
                val uri = Uri.parse(episode.enclosureUrl)
                episodeDuration = uri.getMediaDuration()
            }
        }

        return episodeDuration ?: 0
    }

    @Throws(ArithmeticException::class)
    fun getPlayingProgress(): Int {
        val progress = (currentTime.toLong() * 100) / getCurrentEpisodeDuration()
        return progress.toInt()
    }

    fun didStartPlayingEpisode(episode: ParcelablePodcastEpisode, time: Int) {
        val didChangeEpisode = this.episodeId != episode.id

        if (didChangeEpisode) {
            this.episodeDuration = null
            this.playingEpisode?.playing = false
            this.playingEpisode = getEpisodeWithId(episode.id)
        }

        this.playingEpisode?.playing = true
        this.episodeId = episode.id
        this.timeSeconds = time

        getCurrentEpisodeDuration()
    }

    fun didSeekTo(time: Int) {
        this.timeSeconds = time
    }

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

    private fun didEndPlayingEpisode(episode: ParcelablePodcastEpisode, nextEpisode: ParcelablePodcastEpisode) {
        episode.playing = false

        this.playingEpisode = nextEpisode
        this.episodeId = nextEpisode.id
        this.episodeDuration = null

        this.timeSeconds = 0

        getCurrentEpisodeDuration()
    }

    fun didPausePlayingEpisode(episode: ParcelablePodcastEpisode) {
        episode.playing = false
    }
}

fun Uri.getMediaDuration(): Long {
    val retriever = MediaMetadataRetriever()
    return try {
        if (Build.VERSION.SDK_INT >= 14) {
            retriever.setDataSource(this.toString(), HashMap<String, String>())
        } else {
            retriever.setDataSource(this.toString())
        }
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()
        duration?.toLongOrNull() ?: 0
    } catch (exception: Exception) {
        0
    }
}

fun PodcastDto.toParcelablePodcast(): ParcelablePodcast {
    val podcastEpisodes: MutableList<ParcelablePodcastEpisode> = ArrayList(episodes.size)

    for (episode in episodes) {
        podcastEpisodes.add(episode.toParcelablePodcastEpisode())
    }

    return ParcelablePodcast(id, title, description, author, image, value.toParcelablePodcastValue(), podcastEpisodes)
}

fun ParcelablePodcast.toPodcast(): Podcast {
    val podcastEpisodes: MutableList<PodcastEpisode> = ArrayList(episodes.size)

    for (episode in episodes) {
        podcastEpisodes.add(episode.toPodcastEpisode())
    }

    return Podcast(id, title, description, author, image, value.toPodcastValue(), podcastEpisodes)
}
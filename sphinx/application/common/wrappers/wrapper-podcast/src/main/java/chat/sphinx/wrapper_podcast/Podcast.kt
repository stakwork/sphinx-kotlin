package chat.sphinx.wrapper_podcast

import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_common.ItemId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.toSat
import kotlin.math.roundToInt


class Podcast(
    val id: Long,
    val title: String,
    val description: String,
    val author: String,
    val image: String,
    val value: PodcastValue,
    val episodes: List<PodcastEpisode>,
) {

    //MetaData
    @Volatile
    var episodeId: Long? = null

    @Volatile
    var timeSeconds: Int? = null

    @Volatile
    var speed: Double = 1.0

    @Volatile
    var satsPerMinute: Long = 0

    //Duration
    @Volatile
    var episodeDuration: Long? = null

    //Current Episode
    @Volatile
    var playingEpisode: PodcastEpisode? = null


    val episodesCount: Int
        get() = episodes.count()

    val currentTime: Int
        get() = timeSeconds ?: 0

    val isPlaying: Boolean
        get() = playingEpisode?.playing ?: false

    fun getEpisodesListCopy(): ArrayList<PodcastEpisode> {
        var episodesList = ArrayList<PodcastEpisode>()

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
        return episodes[0]
    }

    private fun getNextEpisode(id: Long): PodcastEpisode {
        for (i in episodes.indices) {
            if (episodes[i].id == id && i-1 >= 0) {
                return episodes[i-1]
            }
        }
        return episodes[0]
    }

    fun getCurrentEpisodeDuration(
        durationRetrieverHandle: (url: String) -> Long
    ): Long {
        if (episodeDuration == null) {

            if (playingEpisode == null) {
                playingEpisode = getCurrentEpisode()
            }

            playingEpisode?.let { episode ->
                episodeDuration = durationRetrieverHandle(episode.enclosureUrl)
            }
        }

        return episodeDuration ?: 0
    }

    @Throws(ArithmeticException::class)
    fun getPlayingProgress(
        durationRetrieverHandle: (url: String) -> Long
    ): Int {
        val progress = (currentTime.toLong() * 100) / getCurrentEpisodeDuration(durationRetrieverHandle)
        return progress.toInt()
    }

    fun didStartPlayingEpisode(
        episode: PodcastEpisode,
        time: Int,
        durationRetrieverHandle: (url: String) -> Long
    ) {
        val didChangeEpisode = this.episodeId != episode.id

        if (didChangeEpisode) {
            this.episodeDuration = null
            this.playingEpisode?.playing = false
            this.playingEpisode = getEpisodeWithId(episode.id)
        }

        this.playingEpisode?.playing = true
        this.episodeId = episode.id
        this.timeSeconds = time

        getCurrentEpisodeDuration(durationRetrieverHandle)
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

    fun endEpisodeUpdate(
        episodeId: Long,
        durationRetrieverHandle: (url: String) -> Long
    ) {
        playingEpisode?.let { episode ->
            val nextEpisode = getNextEpisode(episodeId)
            didEndPlayingEpisode(episode, nextEpisode, durationRetrieverHandle)
        }
    }

    private fun didEndPlayingEpisode(
        episode: PodcastEpisode,
        nextEpisode: PodcastEpisode,
        durationRetrieverHandle: (url: String) -> Long
    ) {
        episode.playing = false

        this.playingEpisode = nextEpisode
        this.episodeId = nextEpisode.id
        this.episodeDuration = null

        this.timeSeconds = 0

        getCurrentEpisodeDuration(durationRetrieverHandle)
    }

    fun didPausePlayingEpisode(episode: PodcastEpisode) {
        episode.playing = false
    }
}
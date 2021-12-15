package chat.sphinx.wrapper_podcast

import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.ItemId
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.feed.Subscribed
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.toSat
import chat.sphinx.wrapper_common.toItemId
import chat.sphinx.wrapper_feed.FeedAuthor
import chat.sphinx.wrapper_feed.FeedDescription
import chat.sphinx.wrapper_feed.FeedTitle
import kotlin.math.roundToInt


data class Podcast(
    val id: FeedId,
    val title: FeedTitle,
    val description: FeedDescription?,
    val author: FeedAuthor?,
    val image: PhotoUrl?,
    val datePublished: DateTime?,
    val chatId: ChatId,
    val feedUrl: FeedUrl,
    val subscribed: Subscribed
) {

    var model: PodcastModel? = null
    var destinations: List<PodcastDestination> = arrayListOf()
    var episodes: List<PodcastEpisode> = arrayListOf()

    //MetaData
    @Volatile
    var episodeId: String? = null

    @Volatile
    var timeMilliSeconds: Int? = null

    @Volatile
    var speed: Double = 1.0

    @Volatile
    var satsPerMinute: Long = model?.suggested?.value?.toLong() ?: 0

    //Duration
    @Volatile
    var episodeDuration: Long? = null

    //Current Episode
    @Volatile
    var playingEpisode: PodcastEpisode? = null


    val episodesCount: Int
        get() = episodes.count()

    val currentTime: Int
        get() = timeMilliSeconds ?: 0

    val isPlaying: Boolean
        get() = playingEpisode?.playing ?: false

    fun getEpisodesListCopy(): ArrayList<PodcastEpisode> {
        var episodesList = ArrayList<PodcastEpisode>()

        for (episode in this.episodes) {
            val episodeCopy = episode.copy()
            episodeCopy.playing = episode.playing

            episodesList.add(episodeCopy)
        }
        return episodesList
    }

    fun setMetaData(metaData: ChatMetaData) {
        episodeId = metaData.itemId.value
        timeMilliSeconds = metaData.timeSeconds * 1000
        speed = metaData.speed
        satsPerMinute = metaData.satsPerMinute.value

        playingEpisode = getEpisodeWithId(metaData.itemId.value)
    }

    fun getMetaData(
        customAmount: Sat? = null
    ): ChatMetaData =
        ChatMetaData(
            FeedId(episodeId ?: "null"),
            episodeId?.toLongOrNull()?.toItemId() ?: ItemId(-1),
            customAmount ?: satsPerMinute.toSat() ?: Sat(0),
            (timeMilliSeconds ?: 0) / 1000,
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

    private fun getEpisodeWithId(id: String): PodcastEpisode? {
        for (episode in episodes) {
            if (episode.id.value == id) {
                return episode
            }
        }

        return episodes[0]
    }

    private fun getNextEpisode(id: String): PodcastEpisode {
        for (i in episodes.indices) {
            if (episodes[i].id.value == id && i-1 >= 0) {
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
                episodeDuration = durationRetrieverHandle(episode.enclosureUrl.value)
            }
        }

        return episodeDuration ?: 0
    }

    fun setInitialEpisodeDuration(duration: Long) {
        if (episodeDuration == null && duration > 0) {
            episodeDuration = duration
        }
    }

    @Throws(ArithmeticException::class)
    fun getPlayingProgress(
        durationRetrieverHandle: (url: String) -> Long
    ): Int {
        val currentEpisodeDuration = getCurrentEpisodeDuration(durationRetrieverHandle)
        if (currentEpisodeDuration > 0) {
            val progress = (currentTime.toLong() * 100) / currentEpisodeDuration
            return progress.toInt()
        }
        return 0
    }

    fun getPlayingProgress(duration: Int): Int {
        val progress = (currentTime.toLong() * 100) / duration
        return progress.toInt()
    }

    fun didStartPlayingEpisode(
        episode: PodcastEpisode,
        time: Int,
        durationRetrieverHandle: (url: String) -> Long
    ) {
        val episodeId = episode.id.value
        val didChangeEpisode = this.episodeId != episodeId

        if (didChangeEpisode) {
            this.episodeDuration = null
            this.playingEpisode?.playing = false
            this.playingEpisode = getEpisodeWithId(episodeId)
        }

        this.playingEpisode?.playing = true
        this.episodeId = episodeId
        this.timeMilliSeconds = time

        getCurrentEpisodeDuration(durationRetrieverHandle)
    }

    fun didSeekTo(time: Int) {
        this.timeMilliSeconds = time
    }

    fun playingEpisodeUpdate(episodeId: String, time: Int, duration: Long) {
        if (playingEpisode == null) {
            this.episodeId?.let { currentEpisodeId ->
                this.playingEpisode = getEpisodeWithId(currentEpisodeId)
            }
        }

        this.episodeDuration = if (duration > 0) duration else this.episodeDuration

        if (episodeId != playingEpisode?.id?.value) {
            this.playingEpisode?.playing = false

            this.episodeDuration = null
            this.playingEpisode = getEpisodeWithId(episodeId)
        }

        playingEpisode?.let { nnEpisode ->
            nnEpisode.playing = true

            this.episodeId = nnEpisode.id.value
            this.timeMilliSeconds = time
        }
    }

    fun pauseEpisodeUpdate() {
        playingEpisode?.let { nnEpisode ->
            didPausePlayingEpisode(nnEpisode)
        }
    }

    fun endEpisodeUpdate(
        episodeId: String,
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
        this.episodeId = nextEpisode.id.value
        this.episodeDuration = null

        this.timeMilliSeconds = 0

        getCurrentEpisodeDuration(durationRetrieverHandle)
    }

    fun didPausePlayingEpisode(episode: PodcastEpisode) {
        episode.playing = false
    }
}
package chat.sphinx.wrapper_podcast

import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.ItemId
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.feed.Subscribed
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.toSat
import chat.sphinx.wrapper_common.toItemId
import chat.sphinx.wrapper_feed.*
import java.io.File
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

    companion object {
        @Suppress("ObjectPropertyName")
        private const val _17 = 17
        @Suppress("ObjectPropertyName")
        private const val _31 = 31
    }

    override fun equals(other: Any?): Boolean {
        if (other is Podcast) {
            return episodes == other.episodes
        }
        return false
    }

    override fun hashCode(): Int {
        var result = _17
        result = _31 * result + episodes.hashCode()
        return result
    }

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

    val hasDestinations: Boolean
        get() = destinations.isNotEmpty()

    var imageToShow: PhotoUrl? = null
        get() {
            getCurrentEpisode()?.image?.let {
                return it
            }
            return image
        }

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

    fun setCurrentEpisodeWith(episodeId: String) {
        this.playingEpisode?.playing = false

        this.playingEpisode = getEpisodeWithId(episodeId)
        this.episodeDuration = null
        this.timeMilliSeconds = 0
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
            episodeId?.let { episodeId ->
                getEpisodeWithId(episodeId)?.let { episode ->
                    return episode
                }
            }
            return episodes[0]
        }
    }

    fun getLastDownloadedEpisode(): PodcastEpisode? {
        for (episode in episodes) {
            if (episode.downloaded) {
                playingEpisode = episode
                return episode
            }
        }
        return null
    }

    fun getEpisodeWithId(id: String): PodcastEpisode? {
        for (episode in episodes) {
            if (episode.id.value == id) {
                return episode
            }
        }
        return null
    }

    fun getItemRankForEpisodeWithId(id: String): Int {
        episodes.forEachIndexed { index, episode ->
            if (episode.id.value == id) {
                return index + 1
            }
        }

        return 0
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
        durationRetrieverHandler: (url: String, localFile: File?) -> Long
    ): Long {
        if (episodeDuration == null) {

            if (playingEpisode == null) {
                playingEpisode = getCurrentEpisode()
            }

            playingEpisode?.let { episode ->

                episodeDuration = durationRetrieverHandler(
                    episode.enclosureUrl.value,
                    episode.localFile
                )
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
        durationRetrieverHandle: (url: String, localFile: File?) -> Long
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
        durationRetrieverHandle: (url: String, localFile: File?) -> Long
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

    fun playingEpisodeUpdate(
        episodeId: String,
        time: Int,
        duration: Long,
        speed: Double
    ) {
        this.speed = speed

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
        durationRetrieverHandle: (url: String, localFile: File?) -> Long
    ) {
        playingEpisode?.let { episode ->
            val nextEpisode = getNextEpisode(episodeId)
            didEndPlayingEpisode(episode, nextEpisode, durationRetrieverHandle)
        }
    }

    private fun didEndPlayingEpisode(
        episode: PodcastEpisode,
        nextEpisode: PodcastEpisode,
        durationRetrieverHandle: (url: String, localFile: File?) -> Long
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

    fun getFeedDestinations(
        clipSenderPubKey: LightningNodePubKey? = null,
    ): List<FeedDestination> {
        val feedDestinations = mutableListOf<FeedDestination>()
        destinations.forEach {
            feedDestinations.add(
                FeedDestination(
                    it.address,
                    it.split,
                    it.type,
                    it.podcastId
                )
            )
        }

        clipSenderPubKey?.let {
            feedDestinations.add(
                FeedDestination(
                    FeedDestinationAddress(it.value),
                    FeedDestinationSplit(1.0),
                    FeedDestinationType("node"),
                    this.id
                )
            )
        }

        return feedDestinations
    }
}
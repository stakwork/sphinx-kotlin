package chat.sphinx.wrapper_podcast

import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_common.DateTime
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedUrl
import chat.sphinx.wrapper_common.feed.Subscribed
import chat.sphinx.wrapper_common.lightning.LightningNodePubKey
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.lightning.toSat
import chat.sphinx.wrapper_feed.*
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
    val subscribed: Subscribed,
    var forceUpdate: Boolean = false
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

    var chat: Chat? = null

    var model: FeedModel? = null
    var destinations: List<FeedDestination> = arrayListOf()

    var contentFeedStatus: ContentFeedStatus? = null

    var episodes: List<PodcastEpisode> = arrayListOf()

    var episodeId: String?
        get() {
            getUpdatedContentFeedStatus()?.itemId?.value?.let {
                if (it.isNotEmpty()) {
                    return it
                }
            }
            return null
        }
        set(value) {
            value?.let {
                playingEpisode = getEpisodeWithId(it)

                contentFeedStatus = getUpdatedContentFeedStatus()?.copy(
                    itemId = FeedId(value)
                )
            }
        }

    var timeMilliSeconds: Long
        get() {
            playingEpisode?.getUpdatedContentEpisodeStatus()?.currentTime?.value?.let {
                return it * 1000
            }
            return 0
        }
        set(value) {
            value?.let {
                playingEpisode?.contentEpisodeStatus = playingEpisode?.getUpdatedContentEpisodeStatus()?.copy(
                    currentTime = FeedItemDuration(it / 1000)
                )
            }
        }

    var speed: Double
        get() {
            getUpdatedContentFeedStatus().playerSpeed?.value?.let {
                return it
            }
            return 1.0
        }
        set(value) {
            value?.let {
                contentFeedStatus = getUpdatedContentFeedStatus()?.copy(
                    playerSpeed = FeedPlayerSpeed(value)
                )
            }
        }

    var satsPerMinute: Long
        get() {
            getUpdatedContentFeedStatus()?.satsPerMinute?.value?.let {
                return it
            }
            return 0
        }
        set(value) {
            value?.let {
                contentFeedStatus = getUpdatedContentFeedStatus()?.copy(
                    satsPerMinute = Sat(value)
                )
            }
        }

    private var episodeDurationMilliseconds: Long
        get() {
            playingEpisode?.getUpdatedContentEpisodeStatus()?.duration?.value?.let { it
                if (it > 0) {
                    return it * 1000
                }
            }
            return 0
        }
        set(value) {
            value?.let {
                playingEpisode?.contentEpisodeStatus = playingEpisode?.getUpdatedContentEpisodeStatus()?.copy(
                    duration = FeedItemDuration(it / 1000)
                )
            }
        }

    private var played: Boolean?
        get() {
          return playingEpisode?.getUpdatedContentEpisodeStatus()?.played
        }
        set(value) {
            value?.let {
                playingEpisode?.contentEpisodeStatus = playingEpisode?.getUpdatedContentEpisodeStatus()?.copy(
                    played = it
                )
            }
        }

    @Volatile
    var playingEpisode: PodcastEpisode? = null
        get() {
            return episodeId?.let {
                getEpisodeWithId(it)
            }
        }

    val shouldLoadDuration: Boolean
        get() = episodeDurationMilliseconds == 0.toLong()

    val episodesCount: Int
        get() = episodes.count()

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

    fun applyPlayingContentState(
        playingContent: Triple<String, String, Boolean>?
    ) {
        if (playingContent?.first == id.value) {
            episodeId = playingContent?.second
            playingEpisode?.playing = true
        }
    }

    fun getEpisodesListCopy(): ArrayList<PodcastEpisode> {
        var episodesList = ArrayList<PodcastEpisode>()

        for (episode in this.episodes) {
            val episodeCopy = episode.copy()
            episodeCopy.playing = episode.playing
            episodeCopy.contentEpisodeStatus = episode.contentEpisodeStatus
            episodeCopy.played = episode.played

            episodesList.add(episodeCopy)
        }
        return episodesList
    }

    fun getDownloadedEpisodesListCopy(): ArrayList<PodcastEpisode> {
        var episodesList = ArrayList<PodcastEpisode>()

        for (episode in this.episodes) {
            if (episode.downloaded) {
                val episodeCopy = episode.copy()
                episodeCopy.playing = episode.playing
                episodeCopy.contentEpisodeStatus = episode.contentEpisodeStatus
                episodeCopy.played = episode.played

                episodesList.add(episodeCopy)
            }
        }
        return episodesList
    }

    fun setCurrentEpisodeWith(episodeId: String) {
        this.playingEpisode?.playing = false

        this.episodeId = episodeId
    }

    fun getUpdatedContentFeedStatus(
        customAmount: Sat? = null
    ): ContentFeedStatus {
        val cfs = customAmount?.let {
            contentFeedStatus?.copy(
                satsPerMinute = customAmount
            )?.let {
                return it
            }
        } ?: contentFeedStatus

        val defaultSatsPerMinute = (chat?.metaData?.satsPerMinute?.value ?: model?.suggestedSats ?: 0).toSat()

        contentFeedStatus = cfs ?: ContentFeedStatus(
            feedId = this.id,
            feedUrl = this.feedUrl,
            subscriptionStatus = this.subscribed,
            chatId = this.chatId,
            itemId = episodes[0]?.id,
            satsPerMinute = customAmount ?: defaultSatsPerMinute,
            playerSpeed = FeedPlayerSpeed(1.0),
        )

        return contentFeedStatus!!
    }


    fun getUpdatedContentEpisodeStatus(): ContentEpisodeStatus =
        playingEpisode?.getUpdatedContentEpisodeStatus() ?: ContentEpisodeStatus(
            this.id,
            getCurrentEpisode().id,
            FeedItemDuration(0),
            FeedItemDuration(0),
            null
        )

    fun getSpeedString(): String {
        if (speed.roundToInt().toDouble() == speed) {
            return "${speed.toInt()}x"
        }
        return "${String.format("%.1f", speed)}x"
    }

    fun getCurrentEpisode(): PodcastEpisode {
        return playingEpisode ?: episodes[0]
    }

    fun getLastDownloadedEpisode(): PodcastEpisode? {
        for (episode in episodes) {
            if (episode.downloaded) {
                episodeId = episode.id.value
                episodeDurationMilliseconds = episode.durationMilliseconds ?: 0
                timeMilliSeconds = episode.currentTimeMilliseconds ?: 0
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
        durationRetrieverHandler: (episode: PodcastEpisode) -> Long
    ): Long {
        if (episodeDurationMilliseconds == 0.toLong()) {

            playingEpisode?.let { episode ->
                episode?.durationMilliseconds?.let {
                    episodeDurationMilliseconds = it
                } ?: run {
                    val durationMilliseconds = durationRetrieverHandler(
                        episode
                    )
                    episodeDurationMilliseconds = durationMilliseconds
                }
            }
        }
        return episodeDurationMilliseconds
    }

    @Throws(ArithmeticException::class)
    fun getPlayingProgress(
        durationRetrieverHandle: (episode: PodcastEpisode) -> Long
    ): Int {
        val currentEpisodeDuration = getCurrentEpisodeDuration(durationRetrieverHandle)
        if (currentEpisodeDuration > 0) {
            val progress = (timeMilliSeconds * 100) / currentEpisodeDuration
            return progress.toInt()
        }
        return 0
    }

    fun getPlayingProgress(duration: Int): Int {
        val progress = (timeMilliSeconds.toLong() * 100) / duration
        return progress.toInt()
    }

    fun willStartPlayingEpisode(
        episode: PodcastEpisode,
        timeMilliseconds: Long,
        durationRetrieverHandle: (episode: PodcastEpisode) -> Long
    ) {
        val episodeId = episode.id.value
        val didChangeEpisode = this.episodeId != episodeId

        if (didChangeEpisode) {
            this.playingEpisode?.playing = false

            this.episodeId = episodeId
            this.episodeDurationMilliseconds = this.playingEpisode?.durationMilliseconds ?: 0
        }

        this.playingEpisode?.playing = true
        this.timeMilliSeconds = timeMilliseconds

        getCurrentEpisodeDuration(durationRetrieverHandle)
    }

    fun didSeekTo(timeMilliseconds: Long) {
        this.timeMilliSeconds = timeMilliseconds
    }

    fun didChangeSatsPerMinute(sats: Long) {
        this.satsPerMinute = sats
    }

    fun playingEpisodeUpdate(
        episodeId: String,
        timeMilliseconds: Int,
        duration: Long,
        speed: Double
    ) {
        if (episodeId != this.episodeId) {
            this.playingEpisode?.playing = false
        }

        this.speed = speed
        this.episodeId = episodeId

        playingEpisode?.let { nnEpisode ->
            nnEpisode.playing = true

            this.episodeDurationMilliseconds = if (duration > 0) duration else this.episodeDurationMilliseconds
            this.timeMilliSeconds = timeMilliseconds.toLong()
        }
    }

    fun pauseEpisodeUpdate() {
        playingEpisode?.let { nnEpisode ->
            didPausePlayingEpisode(nnEpisode)
        }
    }

    fun endEpisodeUpdate(
        episodeId: String,
        durationRetrieverHandle: (episode: PodcastEpisode) -> Long
    ) {
        playingEpisode?.let { episode ->
            val nextEpisode = getNextEpisode(episodeId)
            didEndPlayingEpisode(episode, nextEpisode, durationRetrieverHandle)
        }
    }

    private fun didEndPlayingEpisode(
        episode: PodcastEpisode,
        nextEpisode: PodcastEpisode,
        durationRetrieverHandle: (episode: PodcastEpisode) -> Long
    ) {
        episode.playing = false

        this.episodeId = nextEpisode.id.value
        this.episodeDurationMilliseconds = nextEpisode.durationMilliseconds ?: 0
        this.timeMilliSeconds = 0
        this.played = true

        getCurrentEpisodeDuration(durationRetrieverHandle)
    }

    fun didPausePlayingEpisode(episode: PodcastEpisode) {
        episode.playing = false
    }

    fun getFeedDestinations(
        clipSenderPubKey: LightningNodePubKey? = null,
    ): List<FeedDestination> {
        val feedDestinations = mutableListOf<FeedDestination>()

        feedDestinations.addAll(destinations)

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
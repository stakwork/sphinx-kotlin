package chat.sphinx.feature_service_media_player_android.service

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_repository_actions.ActionsRepository
import chat.sphinx.concept_repository_feed.FeedRepository
import chat.sphinx.concept_service_media.MediaPlayerServiceState
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.feature_service_media_player_android.MediaPlayerServiceControllerImpl
import chat.sphinx.feature_service_media_player_android.model.PodcastDataHolder
import chat.sphinx.feature_service_media_player_android.service.components.AudioManagerHandler
import chat.sphinx.feature_service_media_player_android.service.components.MediaPlayerNotification
import chat.sphinx.feature_service_media_player_android.util.toServiceActionPlay
import chat.sphinx.feature_sphinx_service.SphinxService
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.e
import chat.sphinx.wrapper_action_track.action_wrappers.ContentConsumedHistoryItem
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.toFeedId
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_feed.FeedItemDuration
import chat.sphinx.wrapper_feed.toFeedPlayerSpeed
import chat.sphinx.wrapper_podcast.FeedRecommendation
import io.matthewnelson.concept_foreground_state.ForegroundState
import io.matthewnelson.concept_foreground_state.ForegroundStateManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.util.*
import kotlin.collections.ArrayList

internal abstract class MediaPlayerService: SphinxService() {

    companion object {
        const val TAG = "MediaPlayerService"
    }

    override val mustComplete: Boolean
        get() = true

    var soundPlaying: Boolean = false

    abstract val serviceContext: Context

    protected abstract val audioManagerHandler: AudioManagerHandler
    protected abstract val foregroundStateManager: ForegroundStateManager
    protected abstract val LOG: SphinxLogger
    internal abstract val mediaServiceController: MediaPlayerServiceControllerImpl
    protected abstract val feedRepository: FeedRepository
    protected abstract val actionsRepository: ActionsRepository

    @Suppress("DEPRECATION")
    private val wifiLock: WifiManager.WifiLock? by lazy {
        (getSystemService(Context.WIFI_SERVICE) as? WifiManager)
            ?.createWifiLock(WifiManager.WIFI_MODE_FULL, this.javaClass.simpleName)
    }

    private val notification: MediaPlayerNotification by lazy {
        MediaPlayerNotification(this)
    }

    @Volatile
    protected var currentState: MediaPlayerServiceState = MediaPlayerServiceState.ServiceActive.ServiceLoading
        private set

    inner class MediaPlayerHolder {
        @Volatile
        private var podData: PodcastDataHolder? = null

        private val audioAttributes: AudioAttributes by lazy {
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        }

        private var trackSecondsConsumed: Long = 0
        private var startTimestamp: Long = 0
        private var currentPauseTime: Long = 0
        private var history: ArrayList<ContentConsumedHistoryItem> = arrayListOf()
        @Synchronized
        fun audioFocusLost() {
            podData?.let { nnData ->
                if (nnData.mediaPlayer.isPlaying) {
                    pausePlayer(
                        chatId = nnData.chatId,
                        episodeId = nnData.episodeId,
                        abandonAudioFocus = false
                    )
                }
            }
        }

        @Synchronized
        fun audioFocusGained() {
            podData?.let { nnData ->
                if (!nnData.mediaPlayer.isPlaying) {
                    try {
                        nnData.mediaPlayer.start()
                    } catch (e: IllegalStateException) {
                        LOG.e(TAG, "AudioFocusGained failed to start MediaPlayer", e)
                        // TODO: Handle Exception
                    }

                    if (nnData.mediaPlayer.isPlaying) {
                        if (stateDispatcherJob?.isActive != true) {
                            startStateDispatcher()
                        }
                        wifiLock?.let { lock ->
                            if (!lock.isHeld) {
                                lock.acquire()
                            }
                        }
                    } else {
                        wifiLock?.let { lock ->
                            if (lock.isHeld) {
                                lock.release()
                            }
                        }
                    }
                }
            }
        }


        @Synchronized
        fun soundIsComingOut(){
            soundPlaying = true
        }

        @Synchronized
        fun soundIsNotComingOut(){
            soundPlaying = false
        }

        private fun trackPodcastConsumed(
            podcastId: String,
            episodeId: String
        ) {
            createHistoryItem()

            val currentHistory: ArrayList<ContentConsumedHistoryItem> = arrayListOf()
            currentHistory.addAll(history)

            if (currentHistory.isNotEmpty()) {
                episodeId.toFeedId()?.let {
                    if (podcastId == FeedRecommendation.RECOMMENDATION_PODCAST_ID) {
                        actionsRepository.trackRecommendationsConsumed(it, currentHistory)
                    } else {
                        actionsRepository.trackMediaContentConsumed(it, currentHistory)
                    }
                }
            }

            history.clear()
            resetTrackSecondsConsumed()
        }

        fun getPlayingContent(): Triple<String, String, Boolean>? {
            podData?.let { nnData ->
                if (!nnData.mediaPlayer.isPlaying) {
                    return null
                }
                return Triple(nnData.podcastId, nnData.episodeId, soundPlaying)
            } ?: run {
                return null
            }
        }

        @Synchronized
        fun processUserAction(userAction: UserAction) {
            @Exhaustive
            when (userAction) {
                is UserAction.AdjustSpeed -> {

                    podData?.let { nnData ->
                        if (
                            nnData.chatId == userAction.chatId &&
                            nnData.episodeId == userAction.contentFeedStatus.itemId?.value
                        ) {
                            try {
                                val playing = nnData.mediaPlayer.isPlaying
                                nnData.setSpeed(userAction.contentFeedStatus.playerSpeed?.value ?: 1.0).also {
                                    nnData.mediaPlayer.playbackParams =
                                        nnData.mediaPlayer.playbackParams.setSpeed(it.toFloat())
                                }
                                if (!playing) {
                                    nnData.mediaPlayer.pause()
                                }
                            } catch (e: IllegalStateException) {
                                LOG.e(TAG, "Failed to adjust speed for MediaPlayer", e)
                                // TODO: Handle Error
                            }
                        }
                    }

                    userAction.contentFeedStatus.apply {
                        feedRepository.updateContentFeedStatus(
                            feedId,
                            feedUrl,
                            subscriptionStatus,
                            userAction.chatId,
                            itemId,
                            satsPerMinute,
                            playerSpeed,
                            true
                        )
                    }

                }
                is UserAction.AdjustSatsPerMinute -> {
                    podData?.let { nnData ->
                        userAction.contentFeedStatus.satsPerMinute?.let { nnData.setSatsPerMinute(it) }
                    }

                    userAction.contentFeedStatus.apply {
                        feedRepository.updateContentFeedStatus(
                            feedId,
                            feedUrl,
                            subscriptionStatus,
                            userAction.chatId,
                            itemId,
                            satsPerMinute,
                            playerSpeed,
                            true
                        )
                    }

                }
                is UserAction.SetPaymentsDestinations -> {
                    podData?.let { nnData ->
                        nnData.setDestinations(userAction.destinations)
                    }
                }
                is UserAction.SendBoost -> {
                    podData?.let { nnData ->
                        if (nnData.destinations.isEmpty() && userAction.destinations.isNotEmpty()) {
                            nnData.setDestinations(userAction.destinations)
                        }

                        feedRepository.streamFeedPayments(
                            userAction.chatId,
                            nnData.podcastId,
                            nnData.episodeId,
                            userAction.contentEpisodeStatus.currentTime.value,
                            userAction.contentFeedStatus.satsPerMinute,
                            userAction.contentFeedStatus.playerSpeed,
                            nnData.destinations
                        )
                    }
                }
                is UserAction.ServiceAction.Pause -> {

                    pausePlayer(
                        chatId = userAction.chatId,
                        episodeId = userAction.episodeId,
                        abandonAudioFocus = true
                    )
                }
                is UserAction.ServiceAction.Play -> {

                    serviceLifecycleScope.launch {
                        feedRepository.updateChatContentSeenAt(userAction.chatId)
                        feedRepository.updateLastPlayed(userAction.contentFeedStatus.feedId)
                    }

                    podData?.let { nnData ->
                        if (nnData.episodeId != userAction.contentEpisodeStatus.itemId.value){
                            trackPodcastConsumed(
                                nnData.podcastId,
                                nnData.episodeId
                            )
                        }

                        setStartTimestamp(nnData.currentTimeMilliSeconds.toLong())

                        if (nnData.chatId != userAction.chatId) {
                            //Podcast has changed. Payments Destinations needs to be set again
                            currentState = MediaPlayerServiceState.ServiceActive.ServiceConnected
                            mediaServiceController.dispatchState(currentState)
                        }

                        if (
                            nnData.chatId == userAction.chatId &&
                            nnData.episodeId == userAction.contentEpisodeStatus.itemId.value
                        ) {

                            if (!nnData.mediaPlayer.isPlaying) {
                                try {
                                    nnData.mediaPlayer.seekTo(userAction.contentEpisodeStatus.currentTime.value.toInt() * 1000)
                                    nnData.setSpeed(userAction.contentFeedStatus.playerSpeed?.value ?: 1.0)

                                    if (audioManagerHandler.requestAudioFocus()) {
                                        nnData.mediaPlayer.playbackParams =
                                            nnData
                                                .mediaPlayer
                                                .playbackParams
                                                .setSpeed(userAction.contentFeedStatus.playerSpeed?.value?.toFloat() ?: 1F)
                                        nnData.mediaPlayer.start()
                                    }
                                } catch (e: IllegalStateException) {
                                    LOG.e(TAG, "Failed to start MediaPlayer", e)
                                    // TODO: Handle Error
                                }
                            }

                            if (nnData.mediaPlayer.isPlaying) {
                                if (stateDispatcherJob?.isActive != true) {
                                    startStateDispatcher()
                                }
                                wifiLock?.let { lock ->
                                    if (!lock.isHeld) {
                                        lock.acquire()
                                    }
                                }
                            } else {
                                wifiLock?.let { lock ->
                                    if (lock.isHeld) {
                                        lock.release()
                                    }
                                }
                            }


                        } else {

                            stateDispatcherJob?.cancel()

                            currentState = MediaPlayerServiceState.ServiceActive.ServiceLoading
                            mediaServiceController.dispatchState(currentState)

                            userAction.contentFeedStatus.apply {
                                feedRepository.updateContentFeedStatus(
                                    FeedId(nnData.podcastId),
                                    nnData.feedUrl,
                                    nnData.subscriptionStatus,
                                    nnData.chatId,
                                    nnData.episodeId.toFeedId(),
                                    nnData.satsPerMinute,
                                    nnData.speed.toFeedPlayerSpeed()
                                )
                            }

                            userAction.contentEpisodeStatus.apply {
                                nnData.episodeId.toFeedId()?.let {episodeId ->
                                    feedRepository.updateContentEpisodeStatus(
                                        FeedId(nnData.podcastId),
                                        episodeId,
                                        FeedItemDuration(nnData.durationSeconds.toLong()),
                                        FeedItemDuration(nnData.currentTimeSeconds.toLong())
                                    )
                                }
                            }


                            createMediaPlayer(userAction, nnData.mediaPlayer)

                        }
                    } ?: createMediaPlayer(userAction, null).also {
                        setStartTimestamp(userAction.contentEpisodeStatus.currentTime.value * 1000)
                    }

                    userAction.contentFeedStatus.apply {
                        feedRepository.updateContentFeedStatus(
                            feedId,
                            feedUrl,
                            subscriptionStatus,
                            userAction.chatId,
                            itemId,
                            satsPerMinute,
                            playerSpeed
                        )
                    }

                    userAction.contentEpisodeStatus.apply {
                        feedRepository.updateContentEpisodeStatus(
                            feedId,
                            itemId,
                            duration,
                            currentTime
                        )
                    }

                }
                is UserAction.ServiceAction.Seek -> {
                    podData?.let { nnPlayer ->
                        if (
                            nnPlayer.chatId == userAction.chatId &&
                            nnPlayer.episodeId == userAction.contentEpisodeStatus.itemId.value
                        ) {
                            try {
                                val newTime = userAction.contentEpisodeStatus.currentTime.value.toInt() * 1000
                                nnPlayer.mediaPlayer.seekTo(newTime)
                                createHistoryItem()
                                setStartTimestamp(newTime.toLong())
                                resetTrackSecondsConsumed()
                                // TODO: Dispatch State
                            } catch (e: IllegalStateException) {
                                LOG.e(
                                    TAG,
                                    "Failed to   seekTo ${userAction.contentEpisodeStatus.currentTime.value * 1000} for MediaPlayer",
                                    e
                                )
                                // TODO: Handle Error
                            }
                        }
                    }

                    userAction.contentEpisodeStatus.apply {
                        feedRepository.updateContentEpisodeStatus(
                            feedId,
                            itemId,
                            duration,
                            currentTime,
                            true
                        )
                    }

                }
                is UserAction.TrackPodcastConsumed -> {
                    podData?.let { nnData ->
                        if (!nnData.mediaPlayer.isPlaying) {
                            trackPodcastConsumed(
                                nnData.podcastId,
                                nnData.episodeId
                            )
                        }
                    }
                }
            }
        }

        private fun pausePlayer(
            chatId: ChatId,
            episodeId: String,
            abandonAudioFocus: Boolean = false
        ) {
            podData?.let { nnData ->
                if (
                    nnData.chatId == chatId &&
                    nnData.episodeId == episodeId
                ) {
                    try {
                        nnData.mediaPlayer.pause()

                        wifiLock?.let { lock ->
                            if (lock.isHeld) {
                                lock.release()
                            }
                        }

                        if (abandonAudioFocus) {
                            audioManagerHandler.abandonAudioFocus()
                        }

                        stateDispatcherJob?.cancel()
                        val currentTime = nnData.currentTimeMilliSeconds

                        currentState = MediaPlayerServiceState.ServiceActive.MediaState.Paused(
                            nnData.chatId,
                            nnData.podcastId,
                            nnData.episodeId,
                            currentTime,
                            nnData.durationMilliSeconds,
                            nnData.mediaPlayer.playbackParams.speed.toDouble()
                        )
                        mediaServiceController.dispatchState(currentState)

                        setPauseTime(nnData.currentTimeMilliSeconds.toLong())

                        feedRepository.updateContentFeedStatus(
                            FeedId(nnData.podcastId),
                            nnData.feedUrl,
                            nnData.subscriptionStatus,
                            nnData.chatId,
                            FeedId(nnData.episodeId),
                            nnData.satsPerMinute,
                            nnData.speed.toFeedPlayerSpeed()
                        )

                        feedRepository.updateContentEpisodeStatus(
                            FeedId(nnData.podcastId),
                            FeedId(nnData.episodeId),
                            FeedItemDuration(nnData.durationMilliSeconds.toLong() / 1000),
                            FeedItemDuration(currentTime.toLong() / 1000),
                            true
                        )

                    } catch (e: IllegalStateException) {
                        LOG.e(TAG, "Failed to pause MediaPlayer", e)
                        // TODO: Handle Error
                    }
                }
            }
        }

        private fun createMediaPlayer(
            userAction: UserAction.ServiceAction.Play,
            mediaPlayer: MediaPlayer?,
        ) {
            val player: MediaPlayer = mediaPlayer.also { it?.reset() } ?: MediaPlayer()

            player.apply {
                setAudioAttributes(audioAttributes)
                setWakeMode(serviceContext.applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                setDataSource(userAction.episodeUrl)
                setOnPreparedListener { mp ->
                    mp.setOnPreparedListener(null)
                    mp.seekTo(userAction.contentEpisodeStatus.currentTime.value.toInt() * 1000)
                    mp.playbackParams = mp.playbackParams.setSpeed(userAction.contentFeedStatus.playerSpeed?.value?.toFloat() ?: 1F)

                    if (audioManagerHandler.requestAudioFocus()) {
                        mp.start()
                    } else {
                        mp.pause()
                    }

                    startStateDispatcher()
                }
            }.let { mp ->
                wifiLock?.let { lock ->
                    if (!lock.isHeld) {
                        lock.acquire()
                    }
                }

                mp.prepareAsync()
                podData = PodcastDataHolder.instantiate(
                    userAction.chatId,
                    userAction.contentFeedStatus.feedId.value,
                    userAction.contentEpisodeStatus.itemId.value,
                    userAction.contentFeedStatus.satsPerMinute ?: Sat(0),
                    mp,
                    userAction.contentFeedStatus.playerSpeed?.value ?: 1.0,
                    userAction.contentFeedStatus.feedUrl,
                    userAction.contentFeedStatus.subscriptionStatus
                )

                mp.setOnErrorListener { _, _, _ ->
                    currentState = MediaPlayerServiceState.ServiceActive.MediaState.Failed(
                        userAction.chatId,
                        userAction.contentFeedStatus.feedId.value,
                        userAction.contentEpisodeStatus.itemId.value,
                        userAction.contentEpisodeStatus.currentTime.value.toInt(),
                        userAction.contentEpisodeStatus.duration.value.toInt(),
                        userAction.contentFeedStatus.playerSpeed?.value ?: 1.0
                    )
                    mediaServiceController.dispatchState(currentState)
                    true
                }
            }
        }

        private var stateDispatcherJob: Job? = null
        private fun startStateDispatcher() {
            stateDispatcherJob?.cancel()
            stateDispatcherJob = serviceLifecycleScope.launch {
                var count = 0.0
                while (isActive) {
                    val speed: Double = podData?.speed ?: 1.0
                    podData?.let { nnData ->
                        val currentTimeMilliseconds = nnData.currentTimeMilliSeconds
                        val currentTimeSeconds = nnData.currentTimeSeconds

                        if (count % 15 == 0.0) {
                            feedRepository.updateContentEpisodeStatus(
                                FeedId(nnData.podcastId),
                                FeedId(nnData.episodeId),
                                FeedItemDuration(nnData.durationSeconds.toLong()),
                                FeedItemDuration(nnData.currentTimeSeconds.toLong()),
                                true
                            )
                        }

                        if (count >= 60.0 * speed) {
                            feedRepository.streamFeedPayments(
                                nnData.chatId,
                                nnData.podcastId,
                                nnData.episodeId,
                                currentTimeSeconds.toLong(),
                                nnData.satsPerMinute,
                                speed.toFeedPlayerSpeed(),
                                nnData.destinations
                            )
                            count = 0.0
                        } else {
                            count += 1.0
                        }

                        if (nnData.mediaPlayer.isPlaying) {
                            currentState = MediaPlayerServiceState.ServiceActive.MediaState.Playing(
                                nnData.chatId,
                                nnData.podcastId,
                                nnData.episodeId,
                                currentTimeMilliseconds,
                                nnData.durationMilliSeconds,
                                nnData.mediaPlayer.playbackParams.speed.toDouble()
                            )
                            trackSecondsConsumed++
                        } else {

                            val state = if (nnData.mediaPlayer.duration <= currentTimeMilliseconds) {
                                MediaPlayerServiceState.ServiceActive.MediaState.Ended(
                                    nnData.chatId,
                                    nnData.podcastId,
                                    nnData.episodeId,
                                    currentTimeMilliseconds,
                                    nnData.durationMilliSeconds,
                                    nnData.mediaPlayer.playbackParams.speed.toDouble()
                                )
                            } else {
                                MediaPlayerServiceState.ServiceActive.MediaState.Paused(
                                    nnData.chatId,
                                    nnData.podcastId,
                                    nnData.episodeId,
                                    currentTimeMilliseconds,
                                    nnData.durationMilliSeconds,
                                    nnData.mediaPlayer.playbackParams.speed.toDouble()
                                )
                            }

                            currentState = state
                            mediaServiceController.dispatchState(state)

                            if (state is MediaPlayerServiceState.ServiceActive.MediaState.Ended) {
                                feedRepository.updateContentEpisodeStatus(
                                    FeedId(nnData.podcastId),
                                    FeedId(nnData.episodeId),
                                    FeedItemDuration(nnData.durationSeconds.toLong()),
                                    FeedItemDuration(nnData.currentTimeSeconds.toLong()),
                                    true
                                )

                                shutDownService()
                            }

                            stateDispatcherJob?.cancelAndJoin()
                        }
                    }
                    mediaServiceController.dispatchState(currentState)

                    delay(1_000_000 / (speed * 1000).toLong())
                }
            }
        }

        @Synchronized
        fun clear() {
            stateDispatcherJob?.cancel()
            currentState = MediaPlayerServiceState.ServiceInactive
            mediaServiceController.dispatchState(currentState)
            notification.clear()
            audioManagerHandler.abandonAudioFocus()
            podData?.let { data ->

                feedRepository.updateContentFeedStatus(
                    FeedId(data.podcastId),
                    data.feedUrl,
                    data.subscriptionStatus,
                    data.chatId,
                    data.episodeId.toFeedId(),
                    data.satsPerMinute,
                    data.speed.toFeedPlayerSpeed()
                )

                data.episodeId.toFeedId()?.let { episodeId ->
                    feedRepository.updateContentEpisodeStatus(
                        FeedId(data.podcastId),
                        episodeId,
                        FeedItemDuration(data.durationMilliSeconds.toLong()),
                        FeedItemDuration(data.currentTimeMilliSeconds.toLong())
                    )
                }

                data.mediaPlayer.release()
                podData = null
            }
        }
        private fun resetTrackSecondsConsumed(){
            trackSecondsConsumed = 0
        }
        private fun setStartTimestamp(startTime: Long){
            if (currentPauseTime != startTime) {
                startTimestamp = startTime
            }
        }
        private fun setPauseTime(pauseTime: Long){
            currentPauseTime = pauseTime
        }

        private fun createHistoryItem() {
            if (trackSecondsConsumed > 2) {
                val item = ContentConsumedHistoryItem(
                    arrayListOf(""),
                    startTimestamp,
                    startTimestamp + (trackSecondsConsumed * 1000),
                    Date().time
                )
                history.add(item)
            }
        }
    }

    val mediaPlayerHolder: MediaPlayerHolder by lazy {
        MediaPlayerHolder()
    }

    inner class MediaPlayerServiceBinder: Binder() {
        fun getCurrentState(): MediaPlayerServiceState {
            return currentState
        }

        fun processUserAction(userAction: UserAction) {
            mediaPlayerHolder.processUserAction(userAction)
        }

        fun getPlayingContent(): Triple<String, String, Boolean>? {
            return mediaPlayerHolder.getPlayingContent()
        }
    }

    private val binder: MediaPlayerServiceBinder by lazy(LazyThreadSafetyMode.NONE) {
        MediaPlayerServiceBinder()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    private var rebindJob: Job? = null
    override fun onCreate() {
        super.onCreate()
        mediaServiceController.dispatchState(currentState)
        notification
        rebindJob = serviceLifecycleScope.launch {
            foregroundStateManager.foregroundStateFlow.collect { foregroundState ->
                @Exhaustive
                when (foregroundState) {
                    ForegroundState.Background -> {
                        // AndroidOS automatically unbinds service when
                        // application is sent to the background, so we
                        // rebind it here to ensure we maintain a started,
                        // bound service.
                        mediaServiceController.bindService()
                    }
                    ForegroundState.Foreground -> {}
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.toServiceActionPlay()?.let { mediaPlayerHolder.processUserAction(it) }
        // TODO: If Null, stop service and don't post notification

        return START_NOT_STICKY
//        return super.onStartCommand(intent, flags, startId)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        shutDownService()
    }

    @JvmSynthetic
    fun shutDownService() {
        rebindJob?.cancel()
        wifiLock?.let { lock ->
            if (lock.isHeld) {
                lock.release()
            }
        }
        mediaServiceController.unbindService()
        stopSelf()
    }

    override fun onDestroy() {
        mediaPlayerHolder.clear()
        super.onDestroy()
    }
}

package chat.sphinx.feature_service_media_player_android.service

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import app.cash.exhaustive.Exhaustive
import chat.sphinx.concept_repository_actions.ActionsRepository
import chat.sphinx.concept_repository_media.RepositoryMedia
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
import chat.sphinx.wrapper_chat.ChatMetaData
import chat.sphinx.wrapper_common.ItemId
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.toFeedId
import chat.sphinx.wrapper_common.toItemId
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

    abstract val serviceContext: Context

    protected abstract val audioManagerHandler: AudioManagerHandler
    protected abstract val foregroundStateManager: ForegroundStateManager
    protected abstract val LOG: SphinxLogger
    internal abstract val mediaServiceController: MediaPlayerServiceControllerImpl
    protected abstract val repositoryMedia: RepositoryMedia
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
        private var currentPauseTime: Int = 0
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

        @Synchronized
        fun processUserAction(userAction: UserAction) {
            @Exhaustive
            when (userAction) {
                is UserAction.AdjustSpeed -> {

                    podData?.let { nnData ->
                        if (
                            nnData.chatId == userAction.chatId &&
                            nnData.episodeId == userAction.chatMetaData.itemId.value
                        ) {
                            try {
                                val playing = nnData.mediaPlayer.isPlaying
                                nnData.setSpeed(userAction.chatMetaData.speed).also {
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

                    repositoryMedia.updateChatMetaData(userAction.chatId, null, userAction.chatMetaData)

                }
                is UserAction.AdjustSatsPerMinute -> {
                    podData?.let { nnData ->
                        nnData.setSatsPerMinute(userAction.chatMetaData.satsPerMinute)
                    }

                    repositoryMedia.updateChatMetaData(userAction.chatId, null, userAction.chatMetaData)
                }
                is UserAction.SetPaymentsDestinations -> {
                    podData?.let { nnData ->
                        nnData.setDestinations(userAction.destinations)
                    }
                }
                is UserAction.SendBoost -> {
                    podData?.let { nnData ->
                        repositoryMedia.streamFeedPayments(
                            nnData.chatId,
                            userAction.metaData,
                            nnData.podcastId,
                            nnData.episodeId,
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
                        repositoryMedia.updateChatContentSeenAt(userAction.chatId)
                    }

                    podData?.let { nnData ->
                        if (nnData.episodeId != userAction.episodeId){
                            trackPodcastConsumed(
                                nnData.podcastId,
                                nnData.episodeId
                            )
                        }
                        if (currentPauseTime != nnData.currentTimeSeconds) {
                            setStartTimestamp(nnData.currentTimeMilliSeconds.toLong())
                        }
                        if (nnData.chatId != userAction.chatId) {
                            //Podcast has changed. Payments Destinations needs to be set again
                            currentState = MediaPlayerServiceState.ServiceActive.ServiceConnected
                            mediaServiceController.dispatchState(currentState)
                        }

                        if (
                            nnData.chatId == userAction.chatId &&
                            nnData.episodeId == userAction.episodeId
                        ) {

                            if (!nnData.mediaPlayer.isPlaying) {
                                try {
                                    nnData.mediaPlayer.seekTo(userAction.startTime)
                                    nnData.setSpeed(userAction.speed)

                                    if (audioManagerHandler.requestAudioFocus()) {
                                        nnData.mediaPlayer.playbackParams =
                                            nnData.mediaPlayer.playbackParams.setSpeed(userAction.speed.toFloat())
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

                            repositoryMedia.updateChatMetaData(
                                nnData.chatId,
                                nnData.podcastId.toFeedId(),
                                ChatMetaData(
                                    FeedId(nnData.episodeId),
                                    nnData.episodeId.toLongOrNull()?.toItemId() ?: ItemId(-1),
                                    nnData.satsPerMinute,
                                    nnData.currentTimeSeconds,
                                    nnData.speed
                                )
                            )

                            createMediaPlayer(userAction, nnData.mediaPlayer)

                        }
                    } ?: createMediaPlayer(userAction, null).also {
                        setStartTimestamp(userAction.startTime.toLong())
                    }

                    repositoryMedia.updateChatMetaData(
                        userAction.chatId,
                        userAction.podcastId.toFeedId(),
                        ChatMetaData(
                            FeedId(userAction.episodeId),
                            userAction.episodeId.toLongOrNull()?.toItemId() ?: ItemId(-1),
                            userAction.satPerMinute,
                            userAction.startTime / 1000,
                            userAction.speed
                        )
                    )
                }
                is UserAction.ServiceAction.Seek -> {
                    podData?.let { nnPlayer ->
                        if (
                            nnPlayer.chatId == userAction.chatId &&
                            nnPlayer.episodeId == userAction.chatMetaData.itemId.value
                        ) {
                            try {
                                val secondPosition = userAction.chatMetaData.timeSeconds * 1000
                                nnPlayer.mediaPlayer.seekTo(secondPosition)
                                createHistoryItem()
                                setStartTimestamp(secondPosition.toLong())
                                resetTrackSecondsConsumed()
                                // TODO: Dispatch State
                            } catch (e: IllegalStateException) {
                                LOG.e(
                                    TAG,
                                    "Failed to   seekTo ${userAction.chatMetaData.timeSeconds} for MediaPlayer",
                                    e
                                )
                                // TODO: Handle Error
                            }
                        }
                    }
                    repositoryMedia.updateChatMetaData(userAction.chatId, null, userAction.chatMetaData)
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

                        setPauseTime(nnData.currentTimeSeconds)

                        repositoryMedia.updateChatMetaData(
                            chatId,
                            nnData.podcastId?.toFeedId(),
                            ChatMetaData(
                                FeedId(nnData.episodeId),
                                nnData.episodeId.toLongOrNull()?.toItemId() ?: ItemId(-1),
                                nnData.satsPerMinute,
                                nnData.currentTimeSeconds,
                                nnData.speed,
                            )
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
                    mp.seekTo(userAction.startTime)
                    mp.playbackParams = mp.playbackParams.setSpeed(userAction.speed.toFloat())

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
                    userAction.podcastId,
                    userAction.episodeId,
                    userAction.satPerMinute,
                    mp,
                    userAction.speed
                )

                if (mp.duration == 0) {
                    currentState = MediaPlayerServiceState.ServiceActive.MediaState.Failed(
                        userAction.chatId,
                        userAction.podcastId,
                        userAction.episodeId,
                        0,
                        0,
                        userAction.speed
                    )
                    mediaServiceController.dispatchState(currentState)
                }
            }
        }

        private var stateDispatcherJob: Job? = null
        private fun startStateDispatcher() {
            stateDispatcherJob?.cancel()
            stateDispatcherJob = serviceLifecycleScope.launch {
                var count: Double = 0.0
                while (isActive) {
                    val speed: Double = podData?.speed ?: 1.0
                    podData?.let { nnData ->
                        val currentTimeMilliseconds = nnData.currentTimeMilliSeconds
                        val currentTimeSeconds = nnData.currentTimeSeconds

                        if (count >= 60.0 * speed) {

                            //Chat meta data is updated on relay automatically on stream payments
                            repositoryMedia.streamFeedPayments(
                                nnData.chatId,
                                ChatMetaData(
                                    FeedId(nnData.episodeId),
                                    nnData.episodeId.toLongOrNull()?.toItemId() ?: ItemId(-1),
                                    nnData.satsPerMinute,
                                    currentTimeSeconds,
                                    speed,
                                ),
                                nnData.podcastId,
                                nnData.episodeId,
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
                repositoryMedia.updateChatMetaData(
                    data.chatId,
                    data.podcastId.toFeedId(),
                    ChatMetaData(
                        FeedId(data.episodeId),
                        data.episodeId.toLongOrNull()?.toItemId() ?: ItemId(-1),
                        data.satsPerMinute,
                        data.currentTimeSeconds,
                        data.speed,
                    )
                )
                data.mediaPlayer.release()
                podData = null
            }
        }
        private fun resetTrackSecondsConsumed(){
            trackSecondsConsumed = 0
        }
        private fun setStartTimestamp(startTime: Long){
            startTimestamp = startTime
        }
        private fun setPauseTime(pauseTime: Int){
            currentPauseTime = pauseTime
        }

        private fun createHistoryItem() {
            if (trackSecondsConsumed > 0) {
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

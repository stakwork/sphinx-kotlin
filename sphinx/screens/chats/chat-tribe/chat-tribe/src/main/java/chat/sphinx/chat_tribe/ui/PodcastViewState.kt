package chat.sphinx.chat_tribe.ui

import chat.sphinx.wrapper_podcast.Podcast
import io.matthewnelson.concept_views.viewstate.ViewState
import java.util.*

sealed class PodcastViewState: ViewState<PodcastViewState>() {
    object Idle: PodcastViewState()

    object ServiceLoading: PodcastViewState()
    
    object ServiceInactive: PodcastViewState()

    class PodcastLoaded(
        val podcast: Podcast,
    ): PodcastViewState()

    class PodcastContributionsLoaded(
        val contributions: String,
    ): PodcastViewState()

    class MediaStateUpdate(
        val podcast: Podcast,
    ): PodcastViewState()

}

@Suppress("NOTHING_TO_INLINE")
internal inline fun OnClickCallback.invoke() =
    callback.invoke()

internal class OnClickCallback(val callback: (() -> Unit)) {

    companion object {
        @Suppress("ObjectPropertyName")
        private const val _17 = 17
        @Suppress("ObjectPropertyName")
        private const val _31 = 31
    }

    val uuid = UUID.randomUUID()

    override fun equals(other: Any?): Boolean {
        if (other == null) {
            return false
        }

        return other is OnClickCallback && other.uuid == uuid
    }

    override fun hashCode(): Int {
        var result = _17
        result = _31 * result + uuid.hashCode()
        return result
    }

    override fun toString(): String {
        return "OnClickCallback(uuid=$uuid)"
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun PodcastViewState2.Available.adjustState(
    showLoading: Boolean? = null,
    showPlayButton: Boolean? = null,
    title: String? = null,
    playingProgress: Int? = null,
    clickPlayPause: OnClickCallback? = null,
    clickBoost: OnClickCallback? = null,
    clickFastForward: OnClickCallback? = null,
    clickTitle: OnClickCallback? = null,
): PodcastViewState2.Available =
    PodcastViewState2.Available(
        showLoading = showLoading ?: this.showLoading,
        showPlayButton = showPlayButton ?: this.showPlayButton,
        title = title ?: this.title,
        playingProgress = playingProgress ?: this.playingProgress,
        clickPlayPause = clickPlayPause ?: this.clickPlayPause,
        clickBoost = clickBoost ?: this.clickBoost,
        clickFastForward = clickFastForward ?: this.clickFastForward,
        clickTitle = clickTitle ?: this.clickTitle,
        podcast = podcast
    )

internal sealed class PodcastViewState2: ViewState<PodcastViewState2>() {

    abstract val clickPlayPause: OnClickCallback?
    abstract val clickBoost: OnClickCallback?
    abstract val clickFastForward: OnClickCallback?
    abstract val clickTitle: OnClickCallback?

    object NoPodcast: PodcastViewState2() {
        override val clickPlayPause: OnClickCallback?
            get() = null
        override val clickBoost: OnClickCallback?
            get() = null
        override val clickFastForward: OnClickCallback?
            get() = null
        override val clickTitle: OnClickCallback?
            get() = null
    }

    data class Available(
        val showLoading: Boolean,
        val showPlayButton: Boolean,
        val title: String,
        val playingProgress: Int,
        override val clickPlayPause: OnClickCallback?,
        override val clickBoost: OnClickCallback?,
        override val clickFastForward: OnClickCallback?,
        override val clickTitle: OnClickCallback?,
        val podcast: Podcast,
    ): PodcastViewState2()
}

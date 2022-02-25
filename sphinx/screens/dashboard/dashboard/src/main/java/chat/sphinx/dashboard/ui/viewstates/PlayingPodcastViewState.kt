package chat.sphinx.dashboard.ui.viewstates

import chat.sphinx.wrapper_podcast.Podcast
import io.matthewnelson.concept_views.viewstate.ViewState
import java.util.*

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
internal inline fun PlayingPodcastViewState.PodcastVS.adjustState(
    showLoading: Boolean? = null,
    showPlayButton: Boolean? = null,
    title: String? = null,
    subtitle: String? = null,
    imageUrl: String? = null,
    playingProgress: Int? = null,
    clickPlayPause: OnClickCallback? = null,
    clickBoost: OnClickCallback? = null,
    clickFastForward: OnClickCallback? = null,
    clickTitle: OnClickCallback? = null,
): PlayingPodcastViewState.PodcastVS? {
    return when (this) {
        is PlayingPodcastViewState.PodcastVS.Available -> {
            PlayingPodcastViewState.PodcastVS.Available(
                showLoading = showLoading ?: this.showLoading,
                showPlayButton = showPlayButton ?: this.showPlayButton,
                title = title ?: this.title,
                subtitle = subtitle ?: this.subtitle,
                imageUrl = imageUrl ?: this.imageUrl,
                playingProgress = playingProgress ?: this.playingProgress,
                clickPlayPause = clickPlayPause ?: this.clickPlayPause,
                clickBoost = clickBoost ?: this.clickBoost,
                clickFastForward = clickFastForward ?: this.clickFastForward,
                clickTitle = clickTitle ?: this.clickTitle,
                podcast = podcast
            )
        }
        is PlayingPodcastViewState.PodcastVS.Ready -> {
            PlayingPodcastViewState.PodcastVS.Ready(
                showLoading = showLoading ?: this.showLoading,
                showPlayButton = showPlayButton ?: this.showPlayButton,
                title = title ?: this.title,
                subtitle = subtitle ?: this.subtitle,
                imageUrl = imageUrl ?: this.imageUrl,
                playingProgress = playingProgress ?: this.playingProgress,
                clickPlayPause = clickPlayPause ?: this.clickPlayPause,
                clickBoost = clickBoost ?: this.clickBoost,
                clickFastForward = clickFastForward ?: this.clickFastForward,
                clickTitle = clickTitle ?: this.clickTitle,
                podcast = podcast
            )
        }
        else -> {
            null
        }
    }
}

internal sealed class PlayingPodcastViewState: ViewState<PlayingPodcastViewState>() {

    abstract val clickPlayPause: OnClickCallback?
    abstract val clickBoost: OnClickCallback?
    abstract val clickFastForward: OnClickCallback?
    abstract val clickTitle: OnClickCallback?

    object NoPodcast: PlayingPodcastViewState() {
        override val clickPlayPause: OnClickCallback?
            get() = null
        override val clickBoost: OnClickCallback?
            get() = null
        override val clickFastForward: OnClickCallback?
            get() = null
        override val clickTitle: OnClickCallback?
            get() = null
    }


    abstract class PodcastVS : PlayingPodcastViewState() {

        abstract val showLoading: Boolean
        abstract val showPlayButton: Boolean
        abstract val title: String
        abstract val subtitle: String
        abstract val imageUrl: String?
        abstract val playingProgress: Int
        abstract val podcast: Podcast

        data class Available(
            override val showLoading: Boolean,
            override val showPlayButton: Boolean,
            override val title: String,
            override val subtitle: String,
            override val imageUrl: String?,
            override val playingProgress: Int,
            override val clickPlayPause: OnClickCallback?,
            override val clickBoost: OnClickCallback?,
            override val clickFastForward: OnClickCallback?,
            override val clickTitle: OnClickCallback?,
            override val podcast: Podcast,
        ) : PodcastVS()

        data class Ready(
            override val showLoading: Boolean,
            override val showPlayButton: Boolean,
            override val title: String,
            override val subtitle: String,
            override val imageUrl: String?,
            override val playingProgress: Int,
            override val clickPlayPause: OnClickCallback?,
            override val clickBoost: OnClickCallback?,
            override val clickFastForward: OnClickCallback?,
            override val clickTitle: OnClickCallback?,
            override val podcast: Podcast,
        ) : PodcastVS()
    }
}


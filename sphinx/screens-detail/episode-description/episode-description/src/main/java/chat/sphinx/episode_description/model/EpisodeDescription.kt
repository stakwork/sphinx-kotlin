package chat.sphinx.episode_description.model

import chat.sphinx.wrapper_common.feed.FeedId
import chat.sphinx.wrapper_common.feed.FeedType

data class EpisodeDescription(
    val feedId: FeedId?,
    val header: String,
    val description: String,
    val image: String,
    val episodeType: FeedType?,
    val episodeTypeImage: Int,
    val episodeTypeText: String,
    val episodeDate: String,
    val episodeDuration: String,
    val downloaded: Boolean?,
    var isDownloadInProgress: Boolean?,
    var isEpisodeSoundPlaying: Boolean?,
    val link: String?,
    val played: Boolean,
    val feedName: String?
)

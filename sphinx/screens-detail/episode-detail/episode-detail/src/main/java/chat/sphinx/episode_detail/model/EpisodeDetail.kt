package chat.sphinx.episode_detail.model

data class EpisodeDetail(
    val header: String,
    val image: String,
    val episodeTypeImage: Int,
    val episodeTypeText: String,
    val episodeDate: String,
    val episodeDuration: String,
)
package chat.sphinx.wrapper_podcast

data class PodcastEpisode(
    val id: Long,
    val title: String,
    val description: String,
    val image: String,
    val link: String,
    val enclosureUrl: String,
) {

    var playing: Boolean = false

    var downloaded: Boolean = false
}
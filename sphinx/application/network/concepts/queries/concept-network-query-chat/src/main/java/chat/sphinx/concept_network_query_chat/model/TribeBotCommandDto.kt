package chat.sphinx.concept_network_query_chat.model

import chat.sphinx.wrapper_chat.TribeBot
import chat.sphinx.wrapper_chat.TribeBotCommand
import chat.sphinx.wrapper_podcast.PodcastEpisode
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TribeBotCommandDto(
    val command: String?,
    val price: Long?,
    val min_price: Long?,
    val max_price: Long?,
    val price_index: Long?,
    val admin_only: Boolean
)

fun TribeBotCommandDto.toTribeBotCommand(): TribeBotCommand {
    return TribeBotCommand(command, price, min_price, max_price, price_index, admin_only)
}
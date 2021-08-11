package chat.sphinx.concept_network_query_chat.model

import chat.sphinx.wrapper_chat.TribeBot
import chat.sphinx.wrapper_chat.TribeBotCommand
import chat.sphinx.wrapper_chat.TribeInfo
import chat.sphinx.wrapper_podcast.PodcastEpisode
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TribeBotDto(
    val prefix: String,
    val price: Long,
    val commands: List<TribeBotCommandDto>
)

fun TribeBotDto.toTribeBot(): TribeBot {
    val commandsList: MutableList<TribeBotCommand> = ArrayList(commands.size)

    for (command in commands) {
        commandsList.add(command.toTribeBotCommand())
    }

    return TribeBot(prefix, price, commandsList)
}
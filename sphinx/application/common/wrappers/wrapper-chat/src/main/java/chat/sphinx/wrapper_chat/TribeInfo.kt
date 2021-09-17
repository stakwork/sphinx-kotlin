package chat.sphinx.wrapper_chat

import chat.sphinx.wrapper_common.lightning.Sat
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

class TribeInfo(
    val name: String,
    val description: String,
    val img: String?,
    val tags: List<String>,
    val group_key: String,
    val ownerPubKey: String,
    val ownerRouteHint: String?,
    val ownerAlias: String?,
    val priceToJoin: Long = 0,
    val pricePerMessage: Long = 0,
    val escrowAmount: Long = 0,
    val escrowMillis: Long = 0,
    val unlisted: Boolean?,
    val private: Any?,
    val deleted: Any?,
    val appArl: String?,
    val feedUrl: String?,
    val bots: List<TribeBot>,
) {
    enum class BotPriceError {
        AMOUNT_TOO_LOW,
        AMOUNT_TOO_HIGH
    }

    fun getBotPrice(text: String?): Pair<Sat, BotPriceError?> {
        var price = Sat(0)
        var failureMessage: BotPriceError? = null

        if (text == null || text.isEmpty() || !text.startsWith("/")) {
            return Pair(price, failureMessage)
        }

        for (bot in bots) {
            if (!text.startsWith(bot.prefix)) {
                continue
            }

            if (bot.price > 0) {
                price = Sat(bot.price)
            }

            if (bot.commands.isNotEmpty()) {
                val textComponents = text.split(" ")

                if (textComponents.size < 2) {
                    continue
                }

                run breaker@{
                    bot.commands.forEach { command ->
                        val theCommand = textComponents[1]

                        if (command.command != "*" && theCommand != command.command) {
                            return@forEach
                        }

                        if (command.price != null && command.price > 0) {
                            price = Sat(command.price)
                        } else if (command.priceIndex != null && command.priceIndex > 0) {

                            if (textComponents.size - 1 < command.priceIndex) {
                                return@forEach
                            }

                            textComponents[command.priceIndex.toInt()].toIntOrNull()
                                ?.let { amount ->
                                    if (command.minPrice != null && command.minPrice > 0 && amount < command.minPrice) {
                                        failureMessage = BotPriceError.AMOUNT_TOO_LOW
                                        return@breaker
                                    }
                                    if (command.maxPrice != null && command.maxPrice > 0 && amount > command.maxPrice) {
                                        failureMessage = BotPriceError.AMOUNT_TOO_HIGH
                                        return@breaker
                                    }
                                    price = Sat(amount.toLong())
                                }
                        }
                    }
                }
            }
        }
        return Pair(price, failureMessage)
    }
}

fun String.toTribeBotsList(moshi: Moshi): List<TribeBot> =
    moshi.adapter(Array<TribeBotMoshi>::class.java)
        .fromJson(this)
        ?.let { botMoshiArray ->
            var botsList: MutableList<TribeBot> = mutableListOf()

            for (botMoshi in botMoshiArray) {
                val botCommandsList: MutableList<TribeBotCommand> = mutableListOf()

                for (command in botMoshi.commands) {
                    botCommandsList.add(
                        TribeBotCommand(
                            command.command,
                            command.price,
                            command.minPrice,
                            command.maxPrice,
                            command.priceIndex,
                            command.adminOnly
                        )
                    )
                }

                botsList.add(
                    TribeBot(
                        botMoshi.prefix,
                        botMoshi.price,
                        botCommandsList
                    )
                )
            }

            botsList
        }
        ?: mutableListOf()

@JsonClass(generateAdapter = true)
data class TribeBotMoshi(
    val prefix: String,
    val price: Long,
    val commands: List<TribeBotCommentMoshi>
)

@JsonClass(generateAdapter = true)
data class TribeBotCommentMoshi(
    val command: String?,
    val price: Long?,
    val minPrice: Long?,
    val maxPrice: Long?,
    val priceIndex: Long?,
    val adminOnly: Boolean
)

package chat.sphinx.wrapper_chat

class TribeBotCommand(
    val command: String?,
    val price: Long?,
    val minPrice: Long?,
    val maxPrice: Long?,
    val priceIndex: Long?,
    val adminOnly: Boolean
)
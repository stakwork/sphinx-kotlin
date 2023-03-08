package chat.sphinx.wrapper_badge

data class BadgeTemplate(
    val name: String,
    val description: String = "",
    val rewardType: Int,
    val rewardRequirement: Int,
    val imageUrl: String,
    val chatId: Int
)
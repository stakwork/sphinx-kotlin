package chat.sphinx.wrapper_badge

data class Badge(
    val badgeId: Int?,
    val imageUrl: String?,
    val name: String?,
    val amountCreated: Int?,
    val amountIssued: Int?,
    val chatId: Int?,
    val claimAmount: Int?,
    val rewardType: Int?,
    val rewardRequirement: Int?,
    val description: String?,
    val isActive: Boolean = false
)
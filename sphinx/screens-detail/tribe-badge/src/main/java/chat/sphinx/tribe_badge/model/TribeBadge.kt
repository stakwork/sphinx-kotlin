package chat.sphinx.tribe_badge.model

data class TribeBadge(
    val name: String?,
    val description: String? = null,
    val rewardType: Int?,
    val rewardRequirement: Int?,
    val amount_created: Int? = null,
    val amount_issued: Int? = null,
    val isActive: Boolean? = null,
    val isTemplate: Boolean? = null,
    val imageUrl: String?,
    val manageLabel: Boolean? = null
)

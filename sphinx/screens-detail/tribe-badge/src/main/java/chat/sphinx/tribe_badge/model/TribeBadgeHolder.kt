package chat.sphinx.tribe_badge.model

data class TribeBadgeHolder(
    val name: String,
    val description: String? = null,
    val rewardType: Int? = null,
    val rewardRequirement: Int? = null,
    val amount_created: Int? = null,
    val amount_issued: Int? = null,
    val isActive: Boolean? = null,
    val imageUrl: String? = null,
    val manageLabel: Boolean? = null,
    val holderType: Int? = null
)

sealed class TribeBadgeHolderType {

    companion object {
        const val TEMPLATE = 0
        const val BADGE = 1
        const val HEADER = 2
    }
}

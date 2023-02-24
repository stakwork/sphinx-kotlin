package chat.sphinx.tribe_badge.model

data class TribeBadge(
    val name: String,
    val description: String,
    val isActive: Boolean,
    val imageUrl: String?,
    val manageLabel: Boolean? = null
)

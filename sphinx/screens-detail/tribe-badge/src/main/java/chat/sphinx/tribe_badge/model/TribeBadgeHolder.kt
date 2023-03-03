package chat.sphinx.tribe_badge.model

import chat.sphinx.wrapper_badge.Badge
import chat.sphinx.wrapper_badge.BadgeTemplate

data class TribeBadgeHolder(
    val holderType: Int,
    val headerTitle: String? = null,
    val badge: Badge? = null,
    val badgeTemplate: BadgeTemplate? = null
)

sealed class TribeBadgeHolderType {

    companion object {
        const val TEMPLATE = 0
        const val BADGE = 1
        const val HEADER = 2
    }
}

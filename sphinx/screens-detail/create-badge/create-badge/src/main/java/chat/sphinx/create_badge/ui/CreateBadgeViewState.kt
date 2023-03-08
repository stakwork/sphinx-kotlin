package chat.sphinx.create_badge.ui

import chat.sphinx.wrapper_badge.Badge
import chat.sphinx.wrapper_badge.BadgeTemplate
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class CreateBadgeViewState: ViewState<CreateBadgeViewState>() {

    object Idle : CreateBadgeViewState()

    data class ToggleBadge(
        val badge: Badge
    ): CreateBadgeViewState()

    object LoadingCreateBadge: CreateBadgeViewState()

    data class CreateBadge(
        val badgeTemplate: BadgeTemplate,
        val currentQuantity: Int,
        val pricePerBadge: Int
    ): CreateBadgeViewState()
}
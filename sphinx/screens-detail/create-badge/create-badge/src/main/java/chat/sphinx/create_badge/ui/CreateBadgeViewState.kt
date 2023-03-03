package chat.sphinx.create_badge.ui

import chat.sphinx.wrapper_badge.Badge
import chat.sphinx.wrapper_badge.BadgeTemplate
import io.matthewnelson.concept_views.viewstate.ViewState

sealed class CreateBadgeViewState: ViewState<CreateBadgeViewState>() {

    object Idle : CreateBadgeViewState()

    data class EditBadge(
        val badge: Badge
    ): CreateBadgeViewState()

    data class Template(
        val badgeTemplate: BadgeTemplate
    ): CreateBadgeViewState()

}
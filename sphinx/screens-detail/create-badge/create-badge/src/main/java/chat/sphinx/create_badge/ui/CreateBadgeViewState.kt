package chat.sphinx.create_badge.ui

import io.matthewnelson.concept_views.viewstate.ViewState

sealed class CreateBadgeViewState: ViewState<CreateBadgeViewState>() {

    object Idle : CreateBadgeViewState()

    data class EditBadge(
        val badgeName: String,
        val badgeImage: String,
        val badgeDescription: String,
        val badgeAmount: String,
        val badgeLeft: String,
        val badgeActive: Boolean
    ): CreateBadgeViewState()

}
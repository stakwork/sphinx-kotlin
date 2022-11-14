package chat.sphinx.leaderboard.ui.viewstate

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class LeaderboardViewState: ViewState<LeaderboardViewState>() {

    object Idle: LeaderboardViewState()

}
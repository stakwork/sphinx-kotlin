package chat.sphinx.known_badges.ui

import chat.sphinx.wrapper_badge.Badge
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class KnownBadgesViewState: ViewState<KnownBadgesViewState>() {
    object Idle: KnownBadgesViewState()

    object Loading: KnownBadgesViewState()

    class KnownBadges(
        val badges: List<Badge>
    ): KnownBadgesViewState()
}


package chat.sphinx.splash.ui

import io.matthewnelson.concept_views.viewstate.ViewState

@Suppress("ClassName")
internal sealed class SplashViewState: ViewState<SplashViewState>() {
    object Idle: SplashViewState()
}
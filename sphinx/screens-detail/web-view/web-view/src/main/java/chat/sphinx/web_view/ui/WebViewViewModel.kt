package chat.sphinx.web_view.ui

import chat.sphinx.web_view.navigation.WebViewNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class WebViewViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: WebViewNavigator
): BaseViewModel<WebViewViewState>(dispatchers, WebViewViewState.Idle)
{
}

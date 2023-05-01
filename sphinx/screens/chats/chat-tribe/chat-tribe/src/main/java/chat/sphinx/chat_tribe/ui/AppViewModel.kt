package chat.sphinx.chat_tribe.ui

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import chat.sphinx.chat_tribe.model.TribeFeedData
import chat.sphinx.chat_tribe.ui.viewstate.TribeFeedViewState
import chat.sphinx.wrapper_chat.AppUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class AppViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    handle: SavedStateHandle,
    private val app: Application
) : BaseViewModel<TribeFeedViewState>(dispatchers, TribeFeedViewState.Idle) {

    @Volatile
    private var initialized: Boolean = false
    fun init(appUrl: AppUrl) {
        if (initialized) {
            return
        } else {
            initialized = true
        }
    }

}
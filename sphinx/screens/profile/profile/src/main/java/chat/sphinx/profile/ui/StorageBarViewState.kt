package chat.sphinx.profile.ui

import chat.sphinx.wrapper_common.StoragePercentage
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class StorageBarViewState: ViewState<StorageBarViewState>() {
    object Loading: StorageBarViewState()
    data class StorageData(val storagePercentage: StoragePercentage, val used: String, val total: String): StorageBarViewState()
}

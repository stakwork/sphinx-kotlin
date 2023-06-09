package chat.sphinx.example.manage_storage.viewstate

import chat.sphinx.wrapper_common.StoragePercentage
import chat.sphinx.example.manage_storage.model.StorageSize
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class ManageStorageViewState: ViewState<ManageStorageViewState>() {

    object Loading : ManageStorageViewState()

    data class StorageInfo(
        val storageSize: StorageSize,
        val storagePercentage: StoragePercentage
    ): ManageStorageViewState()

}

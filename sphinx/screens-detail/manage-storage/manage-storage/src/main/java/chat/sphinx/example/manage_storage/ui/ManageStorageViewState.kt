package chat.sphinx.example.manage_storage.ui

import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class ManageStorageViewState: ViewState<ManageStorageViewState>() {

    object Idle : ManageStorageViewState()
    object Loading : ManageStorageViewState()

    data class StorageInfo(
        val usedStorage: String,
        val freeStorage: String,
        val image: String,
        val video: String,
        val audio: String,
        val files: String,
        val chats: String,
        val podcasts: String
    ): ManageStorageViewState()

}

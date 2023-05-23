package chat.sphinx.example.manage_storage.ui

import android.graphics.Bitmap
import io.matthewnelson.concept_views.viewstate.ViewState

internal sealed class ManageStorageViewState: ViewState<ManageStorageViewState>() {

    object Idle : ManageStorageViewState()

    object Loading : ManageStorageViewState()

}

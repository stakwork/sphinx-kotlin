package chat.sphinx.create_tribe.ui

import chat.sphinx.concept_network_query_chat.model.TribeDto
import io.matthewnelson.concept_views.viewstate.ViewState
import java.io.File

internal sealed class CreateTribeViewState: ViewState<CreateTribeViewState>() {
    object Idle: CreateTribeViewState()

    object SavingTribe: CreateTribeViewState()

    class TribeImageUpdated(
        val imageFile: File
    ): CreateTribeViewState()

    object LoadingExistingTribe: CreateTribeViewState()

    class ExistingTribe(
        val tribe: TribeDto
    ): CreateTribeViewState()
}

package chat.sphinx.add_tribe_member.ui

import io.matthewnelson.concept_views.viewstate.ViewState
import java.io.File

internal sealed class AddTribeMemberViewState: ViewState<AddTribeMemberViewState>() {
    object Idle: AddTribeMemberViewState()

    object SavingMember: AddTribeMemberViewState()

    class MemberImageUpdated(
        val imageFile: File
    ): AddTribeMemberViewState()
}

package chat.sphinx.join_tribe.ui

import chat.sphinx.concept_network_query_chat.model.TribeDto
import io.matthewnelson.concept_views.viewstate.ViewState
import java.io.File

internal sealed class JoinTribeViewState: ViewState<JoinTribeViewState>() {
    object LoadingTribe : JoinTribeViewState()
    object ErrorLoadingTribe : JoinTribeViewState()

    data class TribeLoaded(
        val name: String,
        val description: String,
        val imageUrl: String?,
        val priceToJoin: String,
        val pricePerMessage: String,
        val escrowAmount: String,
        val hourToStake: String,
        val myAlias: String?,
        val myPhotoUrl: String?
    ): JoinTribeViewState() {
        companion object {
            operator fun invoke(tribeDto: TribeDto): TribeLoaded =
                TribeLoaded(
                    tribeDto.name,
                    tribeDto.description,
                    tribeDto.img,
                    tribeDto.price_to_join.toString(),
                    tribeDto.price_per_message.toString(),
                    tribeDto.escrow_amount.toString(),
                    tribeDto.hourToStake.toString(),
                    tribeDto.myAlias,
                    tribeDto.myPhotoUrl,
                )
        }
    }

    class TribeProfileImageUpdated(
        val imageFile: File
    ): JoinTribeViewState()

    object JoiningTribe: JoinTribeViewState()
    object ErrorJoiningTribe: JoinTribeViewState()
    object TribeJoined: JoinTribeViewState()
}

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
        val priceToJoin: Long,
        val pricePerMessage: Long,
        val escrowAmount: Long,
        val hourToStake: Long = 0,
        val myAlias: String?,
        val myPhotoUrl: String?
    ): JoinTribeViewState() {
        companion object {
            operator fun invoke(tribeDto: TribeDto): TribeLoaded =
                TribeLoaded(
                    name = tribeDto.name,
                    description = tribeDto.description,
                    imageUrl = tribeDto.img,
                    priceToJoin = tribeDto.price_to_join,
                    pricePerMessage = tribeDto.price_per_message,
                    escrowAmount = tribeDto.escrow_amount,
                    hourToStake = tribeDto.hourToStake,
                    myAlias = tribeDto.myAlias,
                    myPhotoUrl = tribeDto.myPhotoUrl,
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

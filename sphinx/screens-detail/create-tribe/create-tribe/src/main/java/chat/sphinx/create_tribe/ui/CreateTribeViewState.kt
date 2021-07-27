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
        val name: String,
        val description: String,
        val imageUrl: String?,
        val tags: Array<String>,
        val priceToJoin: Long,
        val pricePerMessage: Long,
        val escrowAmount: Long,
        val hourToStake: Long = 0,
        val appUrl: String?,
        val feedUrl: String?,
        val unlisted: Boolean?,
        val private: Any?,
    ): CreateTribeViewState() {
        companion object {
            operator fun invoke(tribeDto: TribeDto): ExistingTribe =
                ExistingTribe(
                    name = tribeDto.name,
                    description = tribeDto.description,
                    imageUrl = tribeDto.img,
                    tags = tribeDto.tags,
                    priceToJoin = tribeDto.price_to_join,
                    pricePerMessage = tribeDto.price_per_message,
                    escrowAmount = tribeDto.escrow_amount,
                    hourToStake = tribeDto.hourToStake,
                    appUrl = tribeDto.app_url,
                    feedUrl = tribeDto.feed_url,
                    unlisted = tribeDto.unlisted,
                    private = tribeDto.private,
                )
        }
    }
}

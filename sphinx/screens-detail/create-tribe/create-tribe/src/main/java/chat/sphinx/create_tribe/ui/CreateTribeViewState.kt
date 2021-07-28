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
        val priceToJoin: String,
        val pricePerMessage: String,
        val escrowAmount: String,
        val hourToStake: String,
        val appUrl: String?,
        val feedUrl: String?,
        val unlisted: Boolean?,
        val private: Any?,
    ): CreateTribeViewState() {
        companion object {
            operator fun invoke(tribeDto: TribeDto): ExistingTribe =
                ExistingTribe(
                    tribeDto.name,
                    tribeDto.description,
                    tribeDto.img,
                    tribeDto.tags,
                    tribeDto.price_to_join.getStringOrEmpty(),
                    tribeDto.price_per_message.getStringOrEmpty(),
                    tribeDto.escrow_amount.getStringOrEmpty(),
                    tribeDto.hourToStake.getStringOrEmpty(),
                    tribeDto.app_url,
                    tribeDto.feed_url,
                    tribeDto.unlisted,
                    tribeDto.private,
                )
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Long.getStringOrEmpty(): String {
    if (this == 0.toLong()) {
        return  ""
    }
    return "$this"
}

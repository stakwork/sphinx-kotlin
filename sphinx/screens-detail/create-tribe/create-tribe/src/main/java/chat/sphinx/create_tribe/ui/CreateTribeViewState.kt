package chat.sphinx.create_tribe.ui

import chat.sphinx.concept_network_query_chat.model.TribeDto
import chat.sphinx.create_tribe.R
import chat.sphinx.wrapper_common.feed.FeedType
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
        val feedTypeDescriptionRes: Int?,
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
                    tribeDto.feed_type?.toFeedTypeDescriptionRes(),
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

@Suppress("NOTHING_TO_INLINE")
private inline fun Int.toFeedTypeDescriptionRes(): Int? {
    when (this) {
        FeedType.Podcast.value -> {
            return R.string.feed_type_podcast
        }
        FeedType.Video.value -> {
            return R.string.feed_type_video
        }
        FeedType.Newsletter.value -> {
            return R.string.feed_type_newsletter
        }
    }
    return null
}

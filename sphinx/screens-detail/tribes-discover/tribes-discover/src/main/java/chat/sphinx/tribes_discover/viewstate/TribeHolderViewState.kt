package chat.sphinx.tribes_discover.viewstate

import chat.sphinx.concept_network_query_chat.model.TribeDto


sealed class TribeHolderViewState(
    val tribeDto: TribeDto? = null
) {
    object Loader : TribeHolderViewState()

    class Tribe(
        tribeDto: TribeDto
    ) : TribeHolderViewState(
        tribeDto
    )
}

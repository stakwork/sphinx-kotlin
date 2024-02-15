package chat.sphinx.tribes_discover.viewstate

import chat.sphinx.concept_network_query_chat.model.NewTribeDto
import chat.sphinx.concept_network_query_chat.model.TribeDto


sealed class TribeHolderViewState(
    val tribeDto: NewTribeDto? = null
) {
    object Loader : TribeHolderViewState()

    class Tribe(
        tribeDto: NewTribeDto
    ) : TribeHolderViewState(
        tribeDto
    )
}

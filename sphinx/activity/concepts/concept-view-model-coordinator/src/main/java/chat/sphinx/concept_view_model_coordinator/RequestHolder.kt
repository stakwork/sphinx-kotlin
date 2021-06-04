package chat.sphinx.concept_view_model_coordinator

import java.util.UUID

class RequestHolder<Request> private constructor(val request: Request) {

    companion object {
        @JvmSynthetic
        internal fun<Request> instantiate(request: Request): RequestHolder<Request> =
            RequestHolder(request)
    }

    val uuid: UUID = UUID.randomUUID()
}

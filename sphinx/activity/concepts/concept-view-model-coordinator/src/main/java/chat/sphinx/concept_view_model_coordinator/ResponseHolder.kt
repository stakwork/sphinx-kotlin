package chat.sphinx.concept_view_model_coordinator

class RequestCancelled<Request>(
    val requestHolder: RequestHolder<Request>
)

class ResponseHolder<Request, Success>(
    val requestHolder: RequestHolder<Request>,
    val response: Success
)

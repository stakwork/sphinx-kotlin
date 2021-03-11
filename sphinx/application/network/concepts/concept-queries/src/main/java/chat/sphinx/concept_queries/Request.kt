package chat.sphinx.concept_queries

abstract class NetworkRequest<T: Request>

abstract class NetworkResponse<T: Response>

sealed class Request {
    object Get: Request()
    object Post: Request()
    object Put: Request()
    object Delete: Request()
}

sealed class Response {
    object Get: Response()
    object Post: Response()
    object Put: Response()
    object Delete: Response()
}
package chat.sphinx.scanner_view_model_coordinator.request

import chat.sphinx.kotlin_response.Response

/**
 * Customize a filter to pass along with the [ScannerRequest]
 * */
abstract class ScannerFilter {

    /**
     * [Response.Success] will return the data to the calling screen.
     * [Response.Error] will display the text to screen.
     *
     * Method is called from Dispatchers.Default
     * */
    abstract suspend fun checkData(data: String): Response<Any, String>
}

package chat.sphinx.wrapper_lightning

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RequestPaymentInvoice(
    val invoice: String,
) {}
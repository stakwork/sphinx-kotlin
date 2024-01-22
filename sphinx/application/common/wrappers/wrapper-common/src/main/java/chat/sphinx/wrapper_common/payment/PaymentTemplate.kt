package chat.sphinx.wrapper_common.payment

class PaymentTemplate(
    val muid: String,
    val width: Int,
    val height: Int,
    val token: String
) {

    fun getTemplateUrl(mediaHost: String): String {
        return "https://$mediaHost/template/$muid"
    }

    fun getDimensions(): String {
        return "{\"dim\":\"${width}x${height}\"}"
    }

    fun getMediaType(): String {
        return "image/png"
    }

}
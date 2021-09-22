package chat.sphinx.wrapper_common.payment

class PaymentTemplate(
    val muid: String,
    val width: Int,
    val height: Int,
    val token: String
) {

    fun getTemplateUrl(mediaHost: String): String? {
        return "$mediaHost/template/$muid"
    }

}
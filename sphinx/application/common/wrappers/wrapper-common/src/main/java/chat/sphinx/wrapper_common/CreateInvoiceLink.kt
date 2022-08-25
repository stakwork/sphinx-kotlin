package chat.sphinx.wrapper_common


@Suppress("NOTHING_TO_INLINE")
inline fun String.toCreateInvoiceLink(): CreateInvoiceLink? =
    try {
        CreateInvoiceLink(this)
    } catch (e: IllegalArgumentException) {
        null
    }

inline val String.isValidCreateInvoiceLink: Boolean
    get() = isNotEmpty() && matches("^${CreateInvoiceLink.REGEX}\$".toRegex())

@JvmInline
value class CreateInvoiceLink(val value: String) {

    companion object {
        const val REGEX = "sphinx\\.chat:\\/\\/\\?action=invoice&amount=.*&secret=.*&name=.*&imgurl=.*"
        const val LINK_NAME = "name"
        const val LINK_AMOUNT = "amount"
        const val LINK_IMAGE = "imageurl"
    }

    init {
        require(value.isValidCreateInvoiceLink) {
            "Invalid Create Invoice Link"
        }
    }

    inline val name : String
        get() = getComponent(LINK_NAME) ?: ""

    inline val amount : Int
        get() {
            val amountString = getComponent(LINK_AMOUNT) ?: ""
            return amountString.toInt()
        }

    inline val image : String
        get() = getComponent(LINK_IMAGE) ?: ""

    fun getComponent(k: String): String? {
        val components = value.replace("sphinx.chat://?", "").split("&")
        for (component in components) {
            val key:String? = component.substringBefore("=")

            if (key != null) {
                val value: String? = component.replace("$key=", "")
                if (key == k) return value
            }
        }
        return null
    }

}
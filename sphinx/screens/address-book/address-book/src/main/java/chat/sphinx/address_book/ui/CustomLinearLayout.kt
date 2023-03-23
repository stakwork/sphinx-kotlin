package chat.sphinx.address_book.ui

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import chat.sphinx.wrapper_message.Message

open class CustomLinearLayout : LinearLayout {

    constructor(context: Context?) : super(null as Context?) {
        throw RuntimeException("Stub!")
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(null as Context?) {
        throw RuntimeException("Stub!")
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(null as Context?) {
        throw RuntimeException("Stub!")
    }

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(null as Context?) {
        throw RuntimeException("Stub!")
    }

    fun addChildren(message: Message) {

    }
}
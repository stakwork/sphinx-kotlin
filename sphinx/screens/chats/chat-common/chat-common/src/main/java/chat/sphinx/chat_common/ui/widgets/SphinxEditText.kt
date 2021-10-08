package chat.sphinx.chat_common.ui.widgets

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.view.inputmethod.EditorInfoCompat
import androidx.core.view.inputmethod.InputConnectionCompat

class SphinxEditText : AppCompatEditText {
    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {}
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {}

    var onCommitContentListener: InputConnectionCompat.OnCommitContentListener? = null

    override fun onCreateInputConnection(editorInfo: EditorInfo): InputConnection {
        // Unless we have a onCommitContentListener we shouldn't override this logic
        onCommitContentListener?.let {
            val ic: InputConnection = super.onCreateInputConnection(editorInfo)!!
            EditorInfoCompat.setContentMimeTypes(editorInfo, arrayOf("image/gif", "image/png"))

            val callback = it

            return InputConnectionCompat.createWrapper(ic, editorInfo, callback)
        }
        Log.w(TAG, "Missing onCommitContentListener to set the content mimeTypes we support")
        return super.onCreateInputConnection(editorInfo)!!
    }

    companion object {
        private const val TAG = "SphinxEditText"
    }
}
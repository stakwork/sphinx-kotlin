package chat.sphinx.resources

import android.content.Context
import android.view.inputmethod.InputMethodManager

inline val Context.inputMethodManager: InputMethodManager?
    get() = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
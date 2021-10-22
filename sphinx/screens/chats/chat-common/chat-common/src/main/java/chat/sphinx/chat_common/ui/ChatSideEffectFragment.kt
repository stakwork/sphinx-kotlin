package chat.sphinx.chat_common.ui

import android.content.Context
import android.view.Window
import androidx.activity.result.ActivityResultLauncher

interface ChatSideEffectFragment {
    val chatFragmentContext: Context
    val chatFragmentWindow: Window?
    val contentChooserContract: ActivityResultLauncher<Array<String>>
}

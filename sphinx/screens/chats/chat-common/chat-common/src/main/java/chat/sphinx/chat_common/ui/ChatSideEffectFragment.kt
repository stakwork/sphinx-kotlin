package chat.sphinx.chat_common.ui

import android.content.Context
import androidx.activity.result.ActivityResultLauncher

interface ChatSideEffectFragment {
    val chatFragmentContext: Context
    val contentChooserContract: ActivityResultLauncher<String>
}

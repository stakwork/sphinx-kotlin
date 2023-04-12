package chat.sphinx.podcast_player.ui

import android.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import chat.sphinx.podcast_player.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed class PodcastPlayerSideEffect: SideEffect<FragmentActivity>() {

    class Notify(
        private val msg: String,
        private val notificationLengthLong: Boolean = true
    ): PodcastPlayerSideEffect() {
        override suspend fun execute(value: FragmentActivity) {
            SphinxToastUtils(toastLengthLong = notificationLengthLong).show(value, msg)
        }
    }

    class CopyLinkSelection(
        private val viewModelScope: CoroutineScope,
        private val callback: suspend (Boolean) -> Unit
    ): PodcastPlayerSideEffect() {
        override suspend fun execute(value: FragmentActivity) {
            val shareFromBeginning = value.getString(R.string.share_from_beginning)
            val shareFromCurrentTime = value.getString(R.string.share_from_current_time)

            val options = arrayOf(shareFromBeginning, shareFromCurrentTime)
            AlertDialog.Builder(value)
                .setItems(options) { _, which ->
                    viewModelScope.launch {
                        when (which) {
                            0 -> callback(false)
                            1 -> callback(true)
                        }
                    }
                }
                .create()
                .show()
        }
    }
}
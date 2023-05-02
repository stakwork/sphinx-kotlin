package chat.sphinx.episode_description.ui

import android.app.AlertDialog
import android.content.Context
import androidx.fragment.app.FragmentActivity
import chat.sphinx.create_description.R
import chat.sphinx.resources.SphinxToastUtils
import io.matthewnelson.android_feature_toast_utils.show
import io.matthewnelson.concept_views.sideeffect.SideEffect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed class EpisodeDescriptionSideEffect: SideEffect<FragmentActivity>() {
    class Notify(
        private val msg: String,
        private val notificationLengthLong: Boolean = true
    ): EpisodeDescriptionSideEffect() {
        override suspend fun execute(value: FragmentActivity) {
            SphinxToastUtils(toastLengthLong = notificationLengthLong).show(value, msg)
        }
    }

    class CopyLinkSelection(
        private val viewModelScope: CoroutineScope,
        private val callback: suspend (Boolean) -> Unit
    ): EpisodeDescriptionSideEffect() {
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

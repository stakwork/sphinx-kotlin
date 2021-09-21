package chat.sphinx.chat_common.ui

import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class MediaPlayerViewModel @Inject constructor(
    private val dispatchers: CoroutineDispatchers
): ViewModel()
{
    val loadingMediaPlayer = MediaPlayer()

    override fun onCleared() {
        super.onCleared()
        loadingMediaPlayer.release()
    }
}

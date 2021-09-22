package chat.sphinx.chat_common.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

internal interface AudioPlayerController {

}

@HiltViewModel
internal class AudioPlayerViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
): ViewModel(), AudioPlayerController, CoroutineDispatchers by dispatchers {

}

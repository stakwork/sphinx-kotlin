package chat.sphinx.qr_code.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import chat.sphinx.qr_code.navigation.QRCodeNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import javax.inject.Inject

@HiltViewModel
internal class QRCodeViewModel @Inject constructor(
    val navigator: QRCodeNavigator,
    dispatchers: CoroutineDispatchers,
    handle: SavedStateHandle,
): SideEffectViewModel<
        Context,
        NotifySideEffect,
        QRCodeViewState
        >(
            dispatchers,
            handle.navArgs<QRCodeFragmentArgs>().let {
                QRCodeViewState.LayoutVisibility(it.value.argShowBackArrow)
            },
        )
{
}

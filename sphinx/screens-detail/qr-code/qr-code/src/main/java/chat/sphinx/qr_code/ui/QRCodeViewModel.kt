package chat.sphinx.qr_code.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import chat.sphinx.qr_code.navigation.BackType
import chat.sphinx.qr_code.navigation.QRCodeNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class QRCodeViewModel @Inject constructor(
    private val navigator: QRCodeNavigator,
    dispatchers: CoroutineDispatchers,
    val savedStateHandle: SavedStateHandle
): SideEffectViewModel<
        Context,
        NotifySideEffect,
        QRCodeViewState
        >(dispatchers, QRCodeViewState.ShowNavBackButton)
{
    val args: QRCodeFragmentArgs by savedStateHandle.navArgs()

    fun goBack(type: BackType) {
        when (type) {
            is BackType.CloseDetailScreen -> {
                viewModelScope.launch(mainImmediate) {
                    navigator.closeDetailScreen()
                }
            }
            is BackType.PopBackStack -> {
                viewModelScope.launch(mainImmediate) {
                    navigator.popBackStack()
                }
            }
        }
    }
}

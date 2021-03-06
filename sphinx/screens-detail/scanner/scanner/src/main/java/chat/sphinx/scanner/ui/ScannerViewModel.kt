package chat.sphinx.scanner.ui

import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import javax.inject.Inject

@HiltViewModel
internal class ScannerViewModel @Inject constructor(

): BaseViewModel<ScannerViewState>(ScannerViewState.Idle)
{
}

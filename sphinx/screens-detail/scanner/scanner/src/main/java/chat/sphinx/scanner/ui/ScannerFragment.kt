package chat.sphinx.scanner.ui

import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.scanner.R
import chat.sphinx.scanner.databinding.FragmentScannerBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment

@AndroidEntryPoint
internal class ScannerFragment: BaseFragment<
        ScannerViewState,
        ScannerViewModel,
        FragmentScannerBinding
        >(R.layout.fragment_scanner)
{
    override val viewModel: ScannerViewModel by viewModels()
    override val binding: FragmentScannerBinding by viewBinding(FragmentScannerBinding::bind)

    override suspend fun onViewStateFlowCollect(viewState: ScannerViewState) {
//        TODO("Not yet implemented")
    }
}

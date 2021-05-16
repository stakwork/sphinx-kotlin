package chat.sphinx.scanner.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.scanner.R
import chat.sphinx.scanner.databinding.FragmentScannerBinding
import chat.sphinx.scanner.navigation.BackType
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // have to call it here so it gets injected and can
        // catch the request asap
        viewModel

        binding.textViewScannerClose.setOnClickListener {
            viewModel.goBack(BackType.CloseDetailScreen)
        }
        binding.textViewScannerHeaderNavBack.setOnClickListener {
            viewModel.goBack(BackType.PopBackStack)
        }
        binding.buttonScannerInputStub.setOnClickListener {
            val input = binding.editTextScannerInputStub.text?.toString()
            if (input != null && input.isNotEmpty()) {
                viewModel.processResponse(ScannerResponse(input))
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: ScannerViewState) {
//        TODO("Not yet implemented")
    }
}

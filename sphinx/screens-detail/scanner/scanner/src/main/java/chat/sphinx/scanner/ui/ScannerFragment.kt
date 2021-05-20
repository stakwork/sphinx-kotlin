package chat.sphinx.scanner.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.scanner.R
import chat.sphinx.scanner.databinding.FragmentScannerBinding
import chat.sphinx.scanner.navigation.BackType
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.invisible
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_viewmodel.updateViewState

@AndroidEntryPoint
internal class ScannerFragment: SideEffectFragment<
        Context,
        NotifySideEffect,
        ScannerViewState,
        ScannerViewModel,
        FragmentScannerBinding
        >(R.layout.fragment_scanner)
{
    private val args: ScannerFragmentArgs by navArgs()
    override val binding: FragmentScannerBinding by viewBinding(FragmentScannerBinding::bind)
    override val viewModel: ScannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // have to call it here so it gets injected and can
        // catch the request asap
        viewModel
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (args.argShowBackArrow) {
            viewModel.updateViewState(ScannerViewState.ShowNavBackButton)
        } else {
            viewModel.updateViewState(ScannerViewState.HideNavBackButton)
        }

        binding.includeScannerHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.scanner_header_name)
            textViewDetailScreenClose.setOnClickListener {
                viewModel.goBack(BackType.CloseDetailScreen)
            }
            textViewDetailScreenHeaderNavBack.setOnClickListener {
                viewModel.goBack(BackType.PopBackStack)
            }
        }

        binding.buttonScannerInputStub.setOnClickListener {
            val input = binding.editTextScannerInputStub.text?.toString()
            if (input != null && input.isNotEmpty()) {
                viewModel.processResponse(ScannerResponse(input))
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: ScannerViewState) {
        binding.includeScannerHeader.textViewDetailScreenHeaderNavBack.apply {
            @Exhaustive
            when (viewState) {
                ScannerViewState.HideNavBackButton -> {
                    invisible
                }
                ScannerViewState.ShowNavBackButton -> {
                    visible
                }
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: NotifySideEffect) {
        sideEffect.execute(binding.root.context)
    }
}

package chat.sphinx.scanner.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.scanner.R
import chat.sphinx.scanner.databinding.FragmentScannerBinding
import chat.sphinx.scanner.navigation.BackType
import chat.sphinx.scanner_view_model_coordinator.response.ScannerResponse
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.invisibleIfFalse
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
internal class ScannerFragment: SideEffectFragment<
        Context,
        NotifySideEffect,
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

        lifecycleScope.launch(viewModel.mainImmediate) {
            try {
                viewModel.requestCatcher.getCaughtRequestStateFlow().collect { list ->
                    list.firstOrNull()?.request?.let { request ->
                        binding.textViewScannerHeaderNavBack.invisibleIfFalse(
                            request.showNavBackArrow
                        )
                        throw Exception()
                    }
                }
            } catch (e: Exception) {}
        }

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

    override suspend fun onSideEffectCollect(sideEffect: NotifySideEffect) {
        sideEffect.execute(binding.root.context)
    }
}

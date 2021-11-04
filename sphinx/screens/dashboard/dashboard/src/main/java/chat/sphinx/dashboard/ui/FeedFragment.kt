package chat.sphinx.dashboard.ui

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.FragmentFeedBinding
import chat.sphinx.dashboard.ui.viewstates.FeedViewState
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.resources.inputMethodManager
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.updateViewState
import kotlinx.coroutines.launch

@Suppress("NOTHING_TO_INLINE")
private inline fun FragmentFeedBinding.searchBarClearFocus() {
    layoutSearchBar.editTextDashboardSearch.clearFocus()
}

@AndroidEntryPoint
internal class FeedFragment : SideEffectFragment<
        Context,
        FeedSideEffect,
        FeedViewState,
        FeedViewModel,
        FragmentFeedBinding
        >(R.layout.fragment_feed)
{
    override val viewModel: FeedViewModel by viewModels()
    override val binding: FragmentFeedBinding by viewBinding(FragmentFeedBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        BackPressHandler(binding.root.context)
            .enableDoubleTapToClose(viewLifecycleOwner, SphinxToastUtils())
            .addCallback(viewLifecycleOwner, requireActivity())

        setupSearch()
    }

    private inner class BackPressHandler(context: Context): CloseAppOnBackPress(context) {
        override fun handleOnBackPressed() {
            if (viewModel.currentViewState !is FeedViewState.Default) {
                viewModel.updateViewState(FeedViewState.Default)
            } else {
                binding.searchBarClearFocus()
                super.handleOnBackPressed()
            }
        }
    }

    private fun setupSearch() {
        binding.layoutSearchBar.apply {
            editTextDashboardSearch.addTextChangedListener { editable ->
                buttonDashboardSearchClear.goneIfFalse(editable.toString().isNotEmpty())

                onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                    // TODO: update viewmodel...
                }
            }

            editTextDashboardSearch.setOnEditorActionListener(object: TextView.OnEditorActionListener {
                override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
                    if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                        editTextDashboardSearch.let { editText ->
                            binding.root.context.inputMethodManager?.let { imm ->
                                if (imm.isActive(editText)) {
                                    imm.hideSoftInputFromWindow(editText.windowToken, 0)
                                    editText.clearFocus()
                                }
                            }
                        }
                        return true
                    }
                    return false
                }
            })

            buttonDashboardSearchClear.setOnClickListener {
                editTextDashboardSearch.setText("")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.searchBarClearFocus()
    }



    override suspend fun onSideEffectCollect(sideEffect: FeedSideEffect) {
        sideEffect.execute(binding.root.context)
    }

    companion object {
        fun newInstance(): FeedFragment {
            return FeedFragment()
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: FeedViewState) {
        // TODO("Not yet implemented")
    }
}

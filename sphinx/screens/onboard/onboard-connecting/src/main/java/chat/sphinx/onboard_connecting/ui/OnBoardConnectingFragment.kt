package chat.sphinx.onboard_connecting.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.onboard_connecting.R
import chat.sphinx.onboard_connecting.databinding.FragmentOnBoardConnectingBinding
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@AndroidEntryPoint
internal class OnBoardConnectingFragment: SideEffectFragment<
        Context,
        OnBoardConnectingSideEffect,
        OnBoardConnectingViewState,
        OnBoardConnectingViewModel,
        FragmentOnBoardConnectingBinding
        >(R.layout.fragment_on_board_connecting)
{
    override val viewModel: OnBoardConnectingViewModel by viewModels()
    override val binding: FragmentOnBoardConnectingBinding by viewBinding(FragmentOnBoardConnectingBinding::bind)

    @Inject
    lateinit var imageLoaderInj: ImageLoader<ImageView>

    private val imageLoader: ImageLoader<ImageView>
        get() = imageLoaderInj

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        BackPressHandler(viewLifecycleOwner, requireActivity())

        setupHeaderAndFooter()

        lifecycleScope.launch {
            imageLoader.load(
                binding.imageViewOnBoardConnecting,
                R.drawable.connecting,
            )
        }
    }

    private fun setupHeaderAndFooter() {
        (requireActivity() as InsetterActivity)
            .addStatusBarPadding(binding.layoutConstraintOnBoardConnecting)
            .addNavigationBarPadding(binding.layoutConstraintOnBoardConnecting)
    }

    private inner class BackPressHandler(
        owner: LifecycleOwner,
        activity: FragmentActivity,
    ): OnBackPressedCallback(true) {

        init {
            activity.apply {
                onBackPressedDispatcher.addCallback(
                    owner,
                    this@BackPressHandler,
                )
            }
        }

        override fun handleOnBackPressed() { }
    }

    override suspend fun onSideEffectCollect(sideEffect: OnBoardConnectingSideEffect) {
        sideEffect.execute(binding.root.context)
    }

    override suspend fun onViewStateFlowCollect(viewState: OnBoardConnectingViewState) {
        //TODO Implement state flow collector
    }
}
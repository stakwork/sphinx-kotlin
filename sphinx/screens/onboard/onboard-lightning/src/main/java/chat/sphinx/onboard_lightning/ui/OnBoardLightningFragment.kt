package chat.sphinx.onboard_lightning.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.onboard_common.model.OnBoardInviterData
import chat.sphinx.onboard_lightning.R
import chat.sphinx.onboard_lightning.databinding.FragmentOnBoardLightningBinding
import chat.sphinx.onboard_lightning.navigation.inviterData
import chat.sphinx.resources.SphinxToastUtils
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class OnBoardLightningFragment: BaseFragment<
        OnBoardLightningViewState,
        OnBoardLightningViewModel,
        FragmentOnBoardLightningBinding
        >(R.layout.fragment_on_board_lightning)
{
    private val args: OnBoardLightningFragmentArgs by navArgs()

    override val viewModel: OnBoardLightningViewModel by viewModels()
    override val binding: FragmentOnBoardLightningBinding by viewBinding(FragmentOnBoardLightningBinding::bind)

    private val inviterData: OnBoardInviterData by lazy(LazyThreadSafetyMode.NONE) { args.inviterData }

    @Inject
    lateinit var imageLoaderInj: ImageLoader<ImageView>

    private val imageLoader: ImageLoader<ImageView>
        get() = imageLoaderInj

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CloseAppOnBackPress(view.context)
            .enableDoubleTapToClose(viewLifecycleOwner, SphinxToastUtils())
            .addCallback(viewLifecycleOwner, requireActivity())

        setupHeaderAndFooter()

        lifecycleScope.launch {
            imageLoader.load(
                binding.imageViewOnBoardLightning,
                R.drawable.lightning_network,
            )
        }

        binding.buttonContinue.setOnClickListener {
            viewModel.nextScreen(inviterData)
        }
    }

    private fun setupHeaderAndFooter() {
        (requireActivity() as InsetterActivity)
            .addStatusBarPadding(binding.layoutConstraintOnBoardConnected)
            .addNavigationBarPadding(binding.layoutConstraintOnBoardConnected)
    }

    override suspend fun onViewStateFlowCollect(viewState: OnBoardLightningViewState) {
        //TODO implement state flow collector
    }
}
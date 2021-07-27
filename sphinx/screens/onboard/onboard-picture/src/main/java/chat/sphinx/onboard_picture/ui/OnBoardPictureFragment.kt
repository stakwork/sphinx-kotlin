package chat.sphinx.onboard_picture.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.onboard_picture.R
import chat.sphinx.onboard_picture.databinding.FragmentOnBoardPictureBinding
import chat.sphinx.resources.SphinxToastUtils
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class OnBoardPictureFragment: SideEffectFragment<
        Context,
        OnBoardPictureSideEffect,
        OnBoardPictureViewState,
        OnBoardPictureViewModel,
        FragmentOnBoardPictureBinding
        >(R.layout.fragment_on_board_picture)
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>
    private val options: ImageLoaderOptions = ImageLoaderOptions.Builder()
        .transformation(Transformation.CircleCrop)
        .build()

    override val viewModel: OnBoardPictureViewModel by viewModels()
    override val binding: FragmentOnBoardPictureBinding by viewBinding(FragmentOnBoardPictureBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        CloseAppOnBackPress(view.context)
            .enableDoubleTapToClose(viewLifecycleOwner, SphinxToastUtils())
            .addCallback(viewLifecycleOwner, requireActivity())

        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(binding.root)
            .addStatusBarPadding(binding.root)
    }

    override suspend fun onSideEffectCollect(sideEffect: OnBoardPictureSideEffect) {
        sideEffect.execute(binding.root.context)
    }

    override suspend fun onViewStateFlowCollect(viewState: OnBoardPictureViewState) {
        @Exhaustive
        when (viewState) {
            is OnBoardPictureViewState.Idle -> {}
            is OnBoardPictureViewState.UserInfo -> {
                viewState.url?.let { url ->
                    imageLoader.load(binding.userProfilePicture, url.value, options)
                }
                binding.textViewOnBoardPictureName.text = viewState.name?.value ?: ""
            }
        }
    }
}

package chat.sphinx.onboard_picture.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.menu_bottom_profile_pic.BottomMenuPicture
import chat.sphinx.onboard_picture.R
import chat.sphinx.onboard_picture.databinding.FragmentOnBoardPictureBinding
import chat.sphinx.onboard_picture.navigation.inviterData
import chat.sphinx.resources.SphinxToastUtils
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.concept_views.viewstate.collect
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class OnBoardPictureFragment: SideEffectFragment<
        Context,
        OnBoardPictureSideEffect,
        OnBoardPictureProgressBarViewState,
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

    private val args: OnBoardPictureFragmentArgs by navArgs()
    override val viewModel: OnBoardPictureViewModel by viewModels()
    override val binding: FragmentOnBoardPictureBinding by viewBinding(FragmentOnBoardPictureBinding::bind)

    private val pictureMenu: BottomMenuPicture by lazy {
        BottomMenuPicture(
            fragment = this,
            onStopSupervisor = onStopSupervisor,
            pictureMenuViewModel = viewModel,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.init(args.argRefreshContacts)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        OnBoardPictureBackPressHandler(view.context)
            .enableDoubleTapToClose(viewLifecycleOwner, SphinxToastUtils())
            .addCallback(viewLifecycleOwner, requireActivity())

        (requireActivity() as InsetterActivity)
            .addStatusBarPadding(binding.layoutConstraintOnBoardPictureInsets)
            .addNavigationBarPadding(binding.layoutConstraintOnBoardPictureInsets)
            .addNavigationBarPadding(binding.includeOnBoardPictureMenuBottomPicture.includeLayoutMenuBottomOptions.root)

        pictureMenu.initialize(
            headerText = R.string.on_board_picture_bottom_menu_header,
            binding = binding.includeOnBoardPictureMenuBottomPicture,
            lifecycleOwner = viewLifecycleOwner
        )

        binding.buttonOnBoardPictureSelectImage.setOnClickListener {
            viewModel.pictureMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Open)
        }

        binding.buttonOnBoardPictureNext.setOnClickListener {
            viewModel.nextScreen(args.inviterData)
        }
    }

    private inner class OnBoardPictureBackPressHandler(context: Context): CloseAppOnBackPress(context) {
        override fun handleOnBackPressed() {
            if (viewModel.pictureMenuHandler.viewStateContainer.value is MenuBottomViewState.Open) {
                viewModel.pictureMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Closed)
            } else {
                super.handleOnBackPressed()
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: OnBoardPictureSideEffect) {
        sideEffect.execute(binding.root.context)
    }

    override suspend fun onViewStateFlowCollect(viewState: OnBoardPictureProgressBarViewState) {
        binding.progressBarOnBoardPictureImageUploading.goneIfFalse(viewState.showProgressBar)
    }

    private var loadImageJob: Job? = null
    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.userInfoViewStateContainer.collect { viewState ->
                @Exhaustive
                when (viewState) {
                    is OnBoardPictureViewState.Idle -> {
                    }
                    is OnBoardPictureViewState.UserInfo -> {
                        viewState.url?.let { url ->
                            loadImageJob?.cancel()
                            loadImageJob = lifecycleScope.launch(viewModel.mainImmediate) {
                                imageLoader.load(
                                    binding.imageViewOnBoardPictureAvatar,
                                    url.value,
                                    options
                                )
                            }
                        }
                        binding.textViewOnBoardPictureName.text = viewState.name?.value ?: ""
                    }
                }
            }
        }
    }
}

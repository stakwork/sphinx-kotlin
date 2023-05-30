package chat.sphinx.example.delete_media.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.delete.media.R
import chat.sphinx.delete.media.databinding.FragmentDeleteMediaBinding
import chat.sphinx.example.delete_media.adapter.MediaSectionAdapter
import chat.sphinx.example.delete_media.viewstate.DeleteMediaViewState
import chat.sphinx.example.delete_media.viewstate.DeleteNotificationViewState
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class DeleteMediaFragment: SideEffectDetailFragment<
        Context,
        DeleteNotifySideEffect,
        DeleteMediaViewState,
        DeleteMediaViewModel,
        FragmentDeleteMediaBinding
        >(R.layout.fragment_delete_media)
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val binding: FragmentDeleteMediaBinding by viewBinding(FragmentDeleteMediaBinding::bind)
    override val viewModel: DeleteMediaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel
    }

    override fun closeDetailsScreen() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.navigator.closeDetailScreen()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        BackPressHandler(viewLifecycleOwner, requireActivity())
        setupMediaSectionAdapter()
        setUpHeader()
        setClickListeners()

        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(binding.deleteMediaScreen)
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

        override fun handleOnBackPressed() {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.popBackStack()
            }
        }
    }

    private fun setUpHeader() {
        binding.apply {}
    }

    private fun setClickListeners() {
        binding.apply {
            includeManageMediaElementHeader.textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.popBackStack()
                }
            }
            includeLayoutDeleteNotificationScreen.apply {
                buttonDelete.setOnClickListener {
                    viewModel.deleteNotificationViewStateContainer.updateViewState(DeleteNotificationViewState.Deleting)
                }
                buttonGotIt.setOnClickListener {
                    viewModel.deleteNotificationViewStateContainer.updateViewState(DeleteNotificationViewState.Closed)
                }
                buttonCancel.setOnClickListener {
                    viewModel.deleteNotificationViewStateContainer.updateViewState(DeleteNotificationViewState.Closed)
                }
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: DeleteMediaViewState) {
        @Exhaustive
        when (viewState) {
            is DeleteMediaViewState.Loading -> {}
            is DeleteMediaViewState.SectionList -> {}
        }
    }

    override fun subscribeToViewStateFlow() {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.deleteNotificationViewStateContainer.collect { viewState ->
                binding.includeLayoutDeleteNotificationScreen.apply {
                    when (viewState) {
                        is DeleteNotificationViewState.Closed -> {
                            root.gone
                        }
                        is DeleteNotificationViewState.Open -> {
                            root.visible
                            constraintChooseDeleteContainer.visible
                            constraintDeleteProgressContainer.gone
                            constraintDeleteSuccessfullyContainer.gone
                        }
                        is DeleteNotificationViewState.Deleting -> {
                            constraintChooseDeleteContainer.gone
                            constraintDeleteProgressContainer.visible
                            constraintDeleteSuccessfullyContainer.gone
                        }
                        is DeleteNotificationViewState.SuccessfullyDeleted -> {
                            constraintChooseDeleteContainer.gone
                            constraintDeleteProgressContainer.gone
                            constraintDeleteSuccessfullyContainer.visible
                        }
                    }
                }
            }
        }
        super.subscribeToViewStateFlow()
    }

    private fun setupMediaSectionAdapter() {
        binding.recyclerViewStorageElementList.apply {
            val mediaSectionAdapter = MediaSectionAdapter(
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel
            )
            layoutManager = LinearLayoutManager(binding.root.context)
            adapter = mediaSectionAdapter
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: DeleteNotifySideEffect) {
        sideEffect.execute(binding.root.context)
    }
}

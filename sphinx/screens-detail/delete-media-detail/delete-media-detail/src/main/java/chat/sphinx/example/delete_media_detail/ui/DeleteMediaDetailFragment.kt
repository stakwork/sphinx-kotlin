package chat.sphinx.example.delete_media_detail.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.delete.media.detail.R
import chat.sphinx.delete.media.detail.databinding.FragmentDeleteMediaDetailsBinding
import chat.sphinx.example.delete_media_detail.adapter.DeleteMediaAdapter
import chat.sphinx.example.delete_media_detail.adapter.DeleteMediaFooterAdapter
import chat.sphinx.example.delete_media_detail.viewstate.DeleteMediaDetailViewState
import chat.sphinx.example.delete_media_detail.viewstate.DeleteNotificationViewState
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class DeleteMediaDetailFragment: SideEffectDetailFragment<
        Context,
        DeleteDetailNotifySideEffect,
        DeleteMediaDetailViewState,
        DeleteMediaDetailViewModel,
        FragmentDeleteMediaDetailsBinding
        >(R.layout.fragment_delete_media_details)
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val binding: FragmentDeleteMediaDetailsBinding by viewBinding(FragmentDeleteMediaDetailsBinding::bind)
    override val viewModel: DeleteMediaDetailViewModel by viewModels()

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
        setupDeleteMediaDetailsAdapter()
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
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: DeleteMediaDetailViewState) {}

    override fun subscribeToViewStateFlow() {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.deleteNotificationViewStateContainer.collect { viewState ->
                binding.includeLayoutManageStorageDeleteNotification.apply {
                    when (viewState) {
                        is DeleteNotificationViewState.Closed -> {}
                        is DeleteNotificationViewState.Open -> {}
                    }
                    root.setTransitionDuration(300)
                    viewState.transitionToEndSet(root)
                }
            }
        }
        super.subscribeToViewStateFlow()
    }

    private fun setupDeleteMediaDetailsAdapter() {
        val deleteMediaFooterAdapter = DeleteMediaFooterAdapter(requireActivity() as InsetterActivity)
        binding.recyclerViewStorageElementList.apply {
            val deleteMediaAdapter = DeleteMediaAdapter(
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel
            )
            layoutManager = LinearLayoutManager(binding.root.context)
            adapter = ConcatAdapter(deleteMediaAdapter, deleteMediaFooterAdapter)
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: DeleteDetailNotifySideEffect) {
        sideEffect.execute(binding.root.context)
    }
}

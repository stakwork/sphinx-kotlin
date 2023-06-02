package chat.sphinx.delete_chat_media.ui

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
import chat.sphinx.delete.chat.media.R
import chat.sphinx.delete.chat.media.databinding.FragmentDeleteChatMediaBinding
import chat.sphinx.delete_chat_media.viewstate.DeleteChatMediaViewState
import chat.sphinx.delete_chat_media.viewstate.DeleteChatNotificationViewState
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
import io.matthewnelson.concept_views.viewstate.value
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
internal class DeleteChatMediaFragment: SideEffectDetailFragment<
        Context,
        DeleteNotifySideEffect,
        DeleteChatMediaViewState,
        DeleteChatMediaViewModel,
        FragmentDeleteChatMediaBinding
        >(R.layout.fragment_delete_chat_media)
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val binding: FragmentDeleteChatMediaBinding by viewBinding(FragmentDeleteChatMediaBinding::bind)
    override val viewModel: DeleteChatMediaViewModel by viewModels()

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
        override fun handleOnBackPressed() {}
    }

    private fun setUpHeader() {
        binding.apply {
            includeManageMediaElementHeader.textViewHeader.text = getString(R.string.chats)
        }
    }

    private fun setClickListeners() {
        binding.apply {
            includeManageMediaElementHeader.textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.popBackStack()
                }
            }

            includeDeleteNotification.apply {
                buttonDelete.setOnClickListener {
                }
                buttonGotIt.setOnClickListener {
                    viewModel.deleteAllFeedsNotificationViewStateContainer.updateViewState(
                        DeleteChatNotificationViewState.Closed)
                }
                buttonCancel.setOnClickListener {
                    viewModel.deleteAllFeedsNotificationViewStateContainer.updateViewState(DeleteChatNotificationViewState.Closed)
                }
            }

            includeManageMediaElementHeader.buttonHeaderDelete.setOnClickListener {
                viewModel.deleteAllFeedsNotificationViewStateContainer.updateViewState(DeleteChatNotificationViewState.Open)
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: DeleteChatMediaViewState) {
        @Exhaustive
        when (viewState) {
            is DeleteChatMediaViewState.Loading -> {}
            is DeleteChatMediaViewState.ChatList -> {}
        }
    }

    override fun subscribeToViewStateFlow() {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.deleteAllFeedsNotificationViewStateContainer.collect { viewState ->
                binding.includeDeleteNotification.apply {
                    when (viewState) {
                        is DeleteChatNotificationViewState.Closed -> {
                            root.gone
                        }
                        is DeleteChatNotificationViewState.Open -> {
                            root.visible
                            constraintChooseDeleteContainer.visible
                            constraintDeleteProgressContainer.gone
                            constraintDeleteSuccessfullyContainer.gone
                        }
                        is DeleteChatNotificationViewState.Deleting -> {
                            root.visible
                            constraintChooseDeleteContainer.gone
                            constraintDeleteProgressContainer.visible
                            constraintDeleteSuccessfullyContainer.gone
                        }
                        is DeleteChatNotificationViewState.SuccessfullyDeleted -> {
                            root.visible
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

    override suspend fun onSideEffectCollect(sideEffect: DeleteNotifySideEffect) {
        sideEffect.execute(binding.root.context)
    }
}

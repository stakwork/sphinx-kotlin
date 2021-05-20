package chat.sphinx.chat_common.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.annotation.LayoutRes
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import app.cash.exhaustive.Exhaustive
import chat.sphinx.chat_common.R
import chat.sphinx.chat_common.adapters.MessageListAdapter
import chat.sphinx.chat_common.databinding.LayoutChatFooterBinding
import chat.sphinx.chat_common.databinding.LayoutChatHeaderBinding
import chat.sphinx.chat_common.navigation.ChatNavigator
import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.chat_common.ui.viewstate.header.ChatHeaderViewState
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.Response
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.resources.setBackgroundRandomColor
import chat.sphinx.resources.setTextColorExt
import chat.sphinx.wrapper_chat.isTrue
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.lightning.asFormattedString
import chat.sphinx.wrapper_common.lightning.unit
import chat.sphinx.wrapper_common.util.getInitials
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.android_feature_toast_utils.show
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

abstract class BaseChatFragment<
        VB: ViewBinding,
        ARGS: NavArgs,
        VM: ChatViewModel<ARGS>,
        >(@LayoutRes layoutId: Int): BaseFragment<
        ChatHeaderViewState,
        VM,
        VB
        >(layoutId)
{
    protected abstract val footerBinding: LayoutChatFooterBinding
    protected abstract val headerBinding: LayoutChatHeaderBinding
    protected abstract val recyclerView: RecyclerView

    protected abstract val imageLoader: ImageLoader<ImageView>

    protected abstract val chatNavigator: ChatNavigator

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupChatHeaderFooter()

        footerBinding
        headerBinding

        headerBinding.textViewChatHeaderNavBack.setOnClickListener {
            lifecycleScope.launch {
                chatNavigator.popBackStack()
            }
        }

        val messageListAdapter = MessageListAdapter(
            recyclerView,
            viewLifecycleOwner,
            onStopSupervisor,
            viewModel,
            imageLoader
        )
        recyclerView.apply {
            setHasFixedSize(false)
            layoutManager = LinearLayoutManager(binding.root.context)
            adapter = messageListAdapter
        }
    }

    private fun setupChatHeaderFooter() {
        val activity = (requireActivity() as InsetterActivity)

        headerBinding.apply {
            activity.addNavigationBarPadding(footerBinding.root)
                .addStatusBarPadding(root)

            root.layoutParams.height = root.layoutParams.height + activity.statusBarInsetHeight.top
            root.requestLayout()
        }
    }

    private fun setupChatMuted(showToast: Boolean = false) {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.chatDataStateFlow.collect { chatData ->
                if (chatData?.chat == null) {
                    headerBinding.imageViewChatHeaderMuted.gone
                } else {
                    setupChatMuted(chatData.muted.isTrue(), showToast)
                }
            }
        }
    }

    private fun setupChatMuted(muted: Boolean, showToast: Boolean = false) {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            if (muted) {
                imageLoader.load(
                    headerBinding.imageViewChatHeaderMuted,
                    R.drawable.ic_baseline_notifications_off_24
                )

                if (showToast) {
                    SphinxToastUtils().show(binding.root.context, R.string.chat_muted_message)
                }

            } else {
                imageLoader.load(
                    headerBinding.imageViewChatHeaderMuted,
                    R.drawable.ic_baseline_notifications_24
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.chatDataStateFlow.collect { chatData ->
                chatData?.let {
                    setupChatMuted()

                    headerBinding.apply {
                        chatData.photoUrl.let { url ->
                            if (url != null) {
                                layoutChatInitialHolder.textViewInitials.gone
                                setChatImageFromUrl(url)
                            } else {
                                layoutChatInitialHolder.apply {
                                    imageViewChatPicture.gone
                                    textViewInitials.text = chatData.chatName?.getInitials() ?: ""
                                }
                            }
                        }

                        textViewChatHeaderName.text = chatData.chatName ?: ""
                        textViewChatHeaderLock.goneIfFalse(chatData.chat != null)
                    }
                }
            }
        }

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.checkRoute.collect { response ->
                headerBinding.textViewChatHeaderConnectivity.apply {
                    @Exhaustive
                    when (response) {
                        is LoadResponse.Loading -> {
                            setTextColorExt(R.color.washedOutReceivedText)
                        }
                        is Response.Error -> {
                            setTextColorExt(R.color.sphinxOrange)
                        }
                        is Response.Success -> {
                            val colorRes = if (response.value) {
                                R.color.primaryGreen
                            } else {
                                R.color.sphinxOrange
                            }

                            setTextColorExt(colorRes)
                        }
                    }
                }
            }
        }

        headerBinding.imageViewChatHeaderMuted.setOnClickListener {
            viewModel.chatDataStateFlow.value?.let { chatData ->
                setupChatMuted(!chatData.muted.isTrue())

                onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                    viewModel.toggleChatMuted.collect { updatedChat ->

                        chatData.updateChat(updatedChat)
                        viewModel.setChatData(chatData)

                        setupChatMuted(showToast = true)
                    }
                }
            }
        }

        viewModel.readMessages()
    }

    private fun setChatImageFromUrl(photoUrl: PhotoUrl) {
        val options = ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_profile_avatar_circle)
            .transformation(Transformation.CircleCrop)

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            imageLoader.load(
                headerBinding.layoutChatInitialHolder.imageViewChatPicture,
                photoUrl.value,
                options.build()
            )
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: ChatHeaderViewState) {
        @Exhaustive
        when (viewState) {
            is ChatHeaderViewState.Idle -> {}
            is ChatHeaderViewState.Initialized -> {
                headerBinding.apply {
                    layoutChatInitialHolder.apply {
                        @Exhaustive
                        when (viewState.initialHolderViewState) {
                            is InitialHolderViewState.Initials -> {
                                imageViewChatPicture.gone
                                textViewInitials.apply {
                                    text = viewState.initialHolderViewState.initials
                                    setBackgroundRandomColor(
                                        R.drawable.chat_initials_circle,
                                        viewState.initialHolderViewState.color
                                    )
                                }

                            }
                            is InitialHolderViewState.None -> {
                                // TODO: remove when IHVS is refacotred such that Initials and URL
                                // are within their own inner sealed class
                            }
                            is InitialHolderViewState.Url -> {
                                textViewInitials.gone
                                imageLoader.load(
                                    imageViewChatPicture,
                                    viewState.initialHolderViewState.photoUrl.value,
                                    viewModel.imageLoaderDefaults,
                                )
                            }
                        }
                    }

                    textViewChatHeaderName.text = viewState.chatHeaderName
                    textViewChatHeaderLock.goneIfFalse(viewState.showLock)

                    textViewChatHeaderConnectivity.apply {
                        @Exhaustive
                        when (viewState.connectivity) {
                            is LoadResponse.Loading -> {
                                setTextColorExt(R.color.washedOutReceivedText)
                            }
                            is Response.Error -> {
                                setTextColorExt(R.color.sphinxOrange)
                            }
                            is Response.Success -> {
                                val colorRes = if (viewState.connectivity.value) {
                                    R.color.primaryGreen
                                } else {
                                    R.color.sphinxOrange
                                }

                                setTextColorExt(colorRes)
                            }
                        }
                    }

                    viewState.contributions?.let {
                        imageViewChatHeaderContributions.visible
                        textViewChatHeaderContributions.apply {
                            visible
                            @SuppressLint("SetTextI18n")
                            text = getString(R.string.chat_tribe_contributions) + " ${it.asFormattedString()} ${it.unit}"
                        }
                    } ?: let {
                        imageViewChatHeaderContributions.gone
                        textViewChatHeaderContributions.gone
                    }

                    imageViewChatHeaderMuted.apply {
                        viewState.isMuted?.let { muted ->
                            if (muted.isTrue()) {
                                imageLoader.load(
                                    headerBinding.imageViewChatHeaderMuted,
                                    R.drawable.ic_baseline_notifications_off_24
                                )

                                // TODO: Show Toast if was switched from on to off
                                //  this needs to be handled in the ViewModel as a
                                //  side effect as the network call could fail.
                            } else {
                                imageLoader.load(
                                    headerBinding.imageViewChatHeaderMuted,
                                    R.drawable.ic_baseline_notifications_24
                                )
                            }
                        } ?: gone
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.readMessages()
    }
}

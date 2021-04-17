package chat.sphinx.chat_common

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import chat.sphinx.chat_common.navigation.ChatNavigator
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import chat.sphinx.wrapper_chat.isTrue
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.util.getInitials
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisorScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

abstract class BaseChatFragment<
        VB: ViewBinding
        >(@LayoutRes layoutId: Int): BaseFragment<
        ChatViewState,
        ChatViewModel,
        VB
        >(layoutId)
{
    override val viewModel: ChatViewModel by activityViewModels()

    protected abstract val header: ConstraintLayout
    protected abstract val headerChatPicture: ImageView
    protected abstract val headerConnectivityIcon: ImageView
    protected abstract val headerInitials: TextView
    protected abstract val headerLockIcon: ImageView
    protected abstract val headerMute: ImageView
    protected abstract val headerName: TextView
    protected abstract val headerNavBack: ImageView

    protected abstract val footer: ConstraintLayout

    protected abstract val imageLoader: ImageLoader<ImageView>

    protected abstract val chatNavigator: ChatNavigator

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(footer)
            .addStatusBarPadding(header)

        ChatBackPressHandler(binding.root.context)
            .addCallback(viewLifecycleOwner, requireActivity())

        headerNavBack.setOnClickListener {
            lifecycleScope.launch {
                chatNavigator.popBackStack()
            }
        }
    }

    private inner class ChatBackPressHandler(context: Context): CloseAppOnBackPress(context) {
        override fun handleOnBackPressed() {
            lifecycleScope.launch {
                chatNavigator.popBackStack()
            }
        }
    }

    protected val onStopSupervisor: OnStopSupervisorScope by lazy {
        OnStopSupervisorScope(viewLifecycleOwner)
    }

    override fun onStart() {
        super.onStart()
        onStopSupervisor.scope().launch(viewModel.dispatchers.mainImmediate) {
            viewModel.chatDataStateFlow.collect { chatData ->
                if (chatData != null) {

                    if (chatData.muted.isTrue()) {
                        imageLoader.load(headerMute, R.drawable.ic_baseline_notifications_off_24)
                    } else {
                        imageLoader.load(headerMute, R.drawable.ic_baseline_notifications_24)
                    }

                    chatData.photoUrl.let { url ->
                        if (url != null) {
                            headerInitials.goneIfFalse(false)
                            setChatImageFromUrl(url)
                        } else {
                            headerChatPicture.goneIfFalse(false)
                            headerInitials.text = chatData.chatName?.getInitials() ?: ""
                        }
                    }

                    headerName.text = chatData.chatName ?: ""

                    headerLockIcon.goneIfFalse(chatData.chat != null)
                }
            }
        }
    }

    private fun setChatImageFromUrl(photoUrl: PhotoUrl) {
        val options = ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_profile_avatar_circle)
            .transformation(Transformation.CircleCrop)

        onStopSupervisor.scope().launch(viewModel.dispatchers.mainImmediate) {
            imageLoader.load(headerChatPicture, photoUrl.value, options.build())
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: ChatViewState) {}
    override fun subscribeToViewStateFlow() {}
}

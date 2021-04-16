package chat.sphinx.chat_common

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import androidx.viewbinding.ViewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.insetter_activity.addStatusBarPadding
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment

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
    protected abstract val footer: ConstraintLayout

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as InsetterActivity)
            .addNavigationBarPadding(footer)
            .addStatusBarPadding(header)
    }
}

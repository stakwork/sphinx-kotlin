package chat.sphinx.keyboard_inset_fragment

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.LayoutRes
import androidx.viewbinding.ViewBinding
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_views.viewstate.ViewState

abstract class KeyboardInsetBaseFragment<
        VS: ViewState<VS>,
        BVM: BaseViewModel<VS>,
        VB: ViewBinding
        >(@LayoutRes layoutId: Int): BaseFragment<VS, BVM, VB>(layoutId)
{

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addGlobalLayoutChangeListener()
    }

    private var viewHeight: Int = 0
    private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    private fun addGlobalLayoutChangeListener() {
        globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
//            if (viewHeight != binding.root.measuredHeight) {
//                viewHeight = binding.root.measuredHeight
//
//                onViewHeightChanged()
//            }
            onKeyboardToggle()
        }

        binding.root.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)

        binding.root.addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(p0: View?) {}

            override fun onViewDetachedFromWindow(p0: View?) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    p0?.viewTreeObserver?.removeOnGlobalLayoutListener(globalLayoutListener)
                } else {
                    p0?.viewTreeObserver?.removeGlobalOnLayoutListener(globalLayoutListener)
                }
                globalLayoutListener = null
            }
        })
    }

    protected abstract fun onKeyboardToggle()
}
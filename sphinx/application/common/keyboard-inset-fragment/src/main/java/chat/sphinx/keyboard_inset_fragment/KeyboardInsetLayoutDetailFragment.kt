package chat.sphinx.keyboard_inset_fragment

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.LayoutRes
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.viewbinding.ViewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import io.matthewnelson.android_concept_views.MotionLayoutViewState
import io.matthewnelson.android_feature_screens.ui.motionlayout.MotionLayoutFragment
import io.matthewnelson.android_feature_viewmodel.MotionLayoutViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_views.sideeffect.SideEffect
import io.matthewnelson.concept_views.viewstate.ViewState

abstract class KeyboardInsetLayoutDetailFragment<
        T,
        SE: SideEffect<T>,
        VS: ViewState<VS>,
        SVM: SideEffectViewModel<T, SE, VS>,
        VB: ViewBinding
        >(@LayoutRes layoutId: Int): SideEffectDetailFragment<
        T,
        SE,
        VS,
        SVM,
        VB
        >(layoutId)
{
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addGlobalLayoutChangeListener()
    }

    private var isKeyboardVisible: Boolean = false
    private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    private var didToggleKeyboard : Boolean = false
        get() {
            val insetterActivity = (requireActivity() as InsetterActivity)

            return (!isKeyboardVisible && insetterActivity.isKeyboardVisible) ||
                    (isKeyboardVisible && !insetterActivity.isKeyboardVisible)
        }

    private fun addGlobalLayoutChangeListener() {
        val insetterActivity = (requireActivity() as InsetterActivity)

        globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            if (didToggleKeyboard) {
                isKeyboardVisible = insetterActivity.isKeyboardVisible
                onKeyboardToggle()
            }
        }

        binding.root.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)

        binding.root.addOnAttachStateChangeListener(object: View.OnAttachStateChangeListener {

            override fun onViewAttachedToWindow(v: View) {}

            override fun onViewDetachedFromWindow(v: View) {
                removeGlobalOnLayoutListenerOn(v)

            }
        })
    }

    private fun removeGlobalOnLayoutListenerOn(view: View?) {
        view?.viewTreeObserver?.removeOnGlobalLayoutListener(globalLayoutListener)
        globalLayoutListener = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeGlobalOnLayoutListenerOn(binding.root)
    }

    protected abstract fun onKeyboardToggle()
}
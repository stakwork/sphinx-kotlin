package chat.sphinx.keyboard_inset_fragment

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.LayoutRes
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.viewbinding.ViewBinding
import chat.sphinx.insetter_activity.InsetterActivity
import io.matthewnelson.android_concept_views.MotionLayoutViewState
import io.matthewnelson.android_feature_screens.ui.motionlayout.MotionLayoutFragment
import io.matthewnelson.android_feature_viewmodel.MotionLayoutViewModel
import io.matthewnelson.concept_views.sideeffect.SideEffect

abstract class KeyboardInsetMotionLayoutFragment<
        MSC: Any,
        T,
        SE: SideEffect<T>,
        MLVS: MotionLayoutViewState<MLVS>,
        MLVM: MotionLayoutViewModel<MSC, T, SE, MLVS>,
        VB: ViewBinding
        >(@LayoutRes layoutId: Int): MotionLayoutFragment<
        MSC,
        T,
        SE,
        MLVS,
        MLVM,
        VB
        >(layoutId), MotionLayout.TransitionListener
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

            override fun onViewAttachedToWindow(v: View) {
            }

            override fun onViewDetachedFromWindow(v: View) {
                removeGlobalOnLayoutListenerOn(v)
            }
        })
    }

    private fun removeGlobalOnLayoutListenerOn(view: View?) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            view?.viewTreeObserver?.removeOnGlobalLayoutListener(globalLayoutListener)
        } else {
            view?.viewTreeObserver?.removeGlobalOnLayoutListener(globalLayoutListener)
        }
        globalLayoutListener = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        removeGlobalOnLayoutListenerOn(binding.root)
    }

    protected abstract fun onKeyboardToggle()
}
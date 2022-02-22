package chat.sphinx.keyboard_inset_fragment

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.LayoutRes
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.viewbinding.ViewBinding
import io.matthewnelson.android_concept_views.MotionLayoutViewState
import io.matthewnelson.android_feature_screens.ui.motionlayout.MotionLayoutFragment
import io.matthewnelson.android_feature_viewmodel.MotionLayoutViewModel
import io.matthewnelson.concept_views.sideeffect.SideEffect

abstract class KeyboardInsetFragment<
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

    override fun getMotionLayouts(): Array<MotionLayout> {
        return arrayOf()
    }

    override fun onViewCreatedRestoreMotionScene(viewState: MLVS, binding: VB) {}

    private var viewHeight: Int = 0
    private var globalLayoutListener: ViewTreeObserver.OnGlobalLayoutListener? = null

    private fun addGlobalLayoutChangeListener() {
        globalLayoutListener = ViewTreeObserver.OnGlobalLayoutListener {
            if (viewHeight != binding.root.measuredHeight) {
                viewHeight = binding.root.measuredHeight

                onViewHeightChanged()
            }
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

    protected abstract fun onViewHeightChanged()
}
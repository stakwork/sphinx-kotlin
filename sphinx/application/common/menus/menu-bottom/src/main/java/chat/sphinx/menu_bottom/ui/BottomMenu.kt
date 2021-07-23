package chat.sphinx.menu_bottom.ui

import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import chat.sphinx.menu_bottom.databinding.LayoutMenuBottomBinding
import chat.sphinx.menu_bottom.databinding.LayoutMenuBottomOptionHolderBinding
import chat.sphinx.menu_bottom.model.MenuBottomDismiss
import chat.sphinx.menu_bottom.model.MenuBottomOption
import chat.sphinx.resources.getColor
import chat.sphinx.resources.getString
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisor
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.launch

open class BottomMenu(
    dispatchers: CoroutineDispatchers,
    protected val onStopSupervisor: OnStopSupervisor,
    protected val viewStateContainer: ViewStateContainer<MenuBottomViewState>,
) : DefaultLifecycleObserver,
    CoroutineDispatchers by dispatchers {

    class Builder private constructor(
        private val binding: LayoutMenuBottomBinding,
        private val viewStateContainer: ViewStateContainer<MenuBottomViewState>,
        private val build: () -> Unit,
    ) {

        companion object {
            @JvmSynthetic
            internal operator fun invoke(
                binding: LayoutMenuBottomBinding,
                viewStateContainer: ViewStateContainer<MenuBottomViewState>,
                build: () -> Unit
            ): Builder =
                Builder(binding, viewStateContainer, build)
        }


        private var headerText: String? = null
        private var headerSubText: String? = null
        private var options: Set<MenuBottomOption>? = null
        private var dismiss: MenuBottomDismiss? = null

        fun setHeaderText(@StringRes resId: Int) = apply {
            setHeaderText(binding.getString(resId))
        }

        fun setHeaderText(text: String) = apply {
            headerText = text
        }

        fun setHeaderSubText(@StringRes resId: Int) = apply {
            headerSubText = binding.getString(resId)
        }

        fun setOptions(options: Set<MenuBottomOption>) = apply {
            this.options = options
        }

        /**
         * If not set, defaults to:
         *   - text = "Cancel"
         *   - textColor = PrimaryRed
         *   - onClick = close menu
         * */
        fun setDismissOption(dismiss: MenuBottomDismiss) = apply {
            this.dismiss = dismiss
        }

        @Throws(IllegalArgumentException::class)
        fun build() {
            val headerText = headerText
            val headerSubText = headerSubText
            val options = options
            val dismiss = dismiss

            require(options != null && options.isNotEmpty()) {
                "setOptions must not be called"
            }

            binding.includeLayoutMenuBottomOptions.apply {

                // Header
                if (headerText == null && headerSubText == null) {
                    layoutConstraintMenuBottomHeader.visibility = View.GONE
                    includeLayoutMenuBottomOption1.viewMenuBottomSeparator.visibility = View.GONE
                } else {
                    textViewMenuBottomHeaderText.apply headerText@ {
                        if (headerText == null) {
                            this@headerText.visibility = View.GONE
                        } else {
                            this@headerText.text = headerText
                        }
                    }

                    if (headerSubText != null) {
                        textViewMenuBottomHeaderSubtext.text = headerSubText
                    }

                    layoutConstraintMenuBottomHeader.setOnClickListener {
                        // disable click through
                    }
                }

                // Options
                setOptionMenuItem(includeLayoutMenuBottomOption1, options.elementAtOrNull(0))
                setOptionMenuItem(includeLayoutMenuBottomOption2, options.elementAtOrNull(1))
                setOptionMenuItem(includeLayoutMenuBottomOption3, options.elementAtOrNull(2))
                setOptionMenuItem(includeLayoutMenuBottomOption4, options.elementAtOrNull(3))

                // Dismiss
                textViewMenuBottomDismiss.apply dismiss@ {
                    dismiss?.let {
                        this@dismiss.text = binding.getString(it.text)
                        it.textColor?.let { colorRes -> this@dismiss.setTextColor(binding.getColor(colorRes)) }
                    }

                    this@dismiss.setOnClickListener {
                        dismiss?.onClick?.invoke() ?: viewStateContainer.updateViewState(MenuBottomViewState.Closed)
                    }
                }

                // InputLock
                binding.viewMenuBottomInputLock.setOnClickListener {
                    viewStateContainer.updateViewState(MenuBottomViewState.Closed)
                }

                build.invoke()
            }
        }

        private fun setOptionMenuItem(
            holderBinding: LayoutMenuBottomOptionHolderBinding,
            option: MenuBottomOption?
        ) {
            if (option == null) {
                holderBinding.root.visibility = View.GONE
            } else {
                holderBinding.textViewMenuBottomOption.apply {
                    option.textColor?.let { res ->
                        setTextColor(binding.getColor(res))
                    }
                    text = binding.getString(option.text)
                }

                holderBinding.root.setOnClickListener {
                    option.onClick.invoke()
                }
            }
        }
    }

    private var binding: LayoutMenuBottomBinding? = null
    private var hasBeenRestored = false

    open fun newBuilder(
        binding: LayoutMenuBottomBinding,
        lifecycleOwner: LifecycleOwner,
    ): Builder =
        Builder(binding, viewStateContainer) {
            this.binding = binding
            lifecycleOwner.lifecycle.addObserver(this)
        }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        onStopSupervisor.scope.launch(mainImmediate) {
            binding?.let { nnBinding ->
                viewStateContainer.collect { viewState ->

                    when (viewState) {
                        MenuBottomViewState.Closed -> {
                            nnBinding.root.setTransitionDuration(150)
                        }
                        MenuBottomViewState.Open -> {
                            nnBinding.root.setTransitionDuration(250)
                        }
                    }

                    if (hasBeenRestored) {
                        viewState.transitionToEndSet(nnBinding.root)
                    } else {
                        hasBeenRestored = true
                        viewState.restoreMotionScene(nnBinding.root)
                    }
                }
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        hasBeenRestored = false
        binding = null
    }
}

package chat.sphinx.payment_common.ui

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavArgs
import androidx.viewbinding.ViewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.detail_resources.databinding.LayoutDetailScreenHeaderBinding
import chat.sphinx.payment_common.R
import chat.sphinx.payment_common.databinding.LayoutConstraintAmountBinding
import chat.sphinx.payment_common.databinding.LayoutConstraintConfirmButtonBinding
import chat.sphinx.payment_common.databinding.LayoutConstraintFromContactBinding
import chat.sphinx.payment_common.databinding.LayoutConstraintMessageBinding
import chat.sphinx.payment_common.ui.viewstate.AmountViewState
import chat.sphinx.resources.databinding.LayoutAmountPadBinding
import chat.sphinx.resources.getRandomHexCode
import chat.sphinx.resources.setBackgroundRandomColor
import chat.sphinx.screen_detail_fragment.SideEffectDetailFragment
import chat.sphinx.wrapper_common.PhotoUrl
import chat.sphinx.wrapper_common.util.getInitials
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_contact.getColorKey
import chat.sphinx.wrapper_message.SenderAlias
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.ViewState
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

abstract class PaymentFragment<
        VB: ViewBinding,
        ARGS: NavArgs,
        VM: PaymentViewModel<ARGS, VS>,
        VS: ViewState<VS>,
        >(@LayoutRes layoutId: Int): SideEffectDetailFragment<
        FragmentActivity,
        PaymentSideEffect,
        VS,
        VM,
        VB
        >(layoutId)
{
    protected abstract val imageLoader: ImageLoader<ImageView>

    protected abstract val headerBinding: LayoutDetailScreenHeaderBinding
    protected abstract val contactBinding: LayoutConstraintFromContactBinding
    protected abstract val amountBinding: LayoutConstraintAmountBinding
    protected abstract val amountPadBinding: LayoutAmountPadBinding
    protected abstract val messageBinding: LayoutConstraintMessageBinding
    protected abstract val confirmationBinding: LayoutConstraintConfirmButtonBinding

    protected abstract val headerStringId: Int

    @Inject
    @Suppress("ProtectedInFinal", "PropertyName")
    protected lateinit var userColorsHelper: UserColorsHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        headerBinding.apply {
            textViewDetailScreenHeaderName.text = getString(headerStringId)
            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }

        setupFooter()
        setupNumberPad()
    }

    override fun closeDetailsScreen() {
        lifecycleScope.launch(viewModel.mainImmediate) {
            viewModel.navigator.closeDetailScreen()
        }
    }

    protected abstract fun setupFooter();

    protected fun setupDestination(
        contact: Contact? = null,
        memberAlias: SenderAlias? = null,
        memberColorKey: String? = null,
        memberPic: PhotoUrl? = null
    ) {

        contactBinding.apply {
            (contact?.alias?.value ?: memberAlias?.value)?.let { alias ->
                textViewContactName.text = alias

                textViewInitials.apply {
                    text = alias.getInitials()

                    (contact?.getColorKey() ?: memberColorKey)?.let { colorKey ->
                        lifecycleScope.launch(viewModel.mainImmediate) {
                            setBackgroundRandomColor(
                                R.drawable.chat_initials_circle,
                                Color.parseColor(
                                    userColorsHelper.getHexCodeForKey(
                                        colorKey,
                                        root.context.getRandomHexCode(),
                                    )
                                ),
                            )
                        }
                    }
                }
            }

            lifecycleScope.launch(viewModel.mainImmediate) {
                (contact?.photoUrl?.value ?: memberPic?.value)?.let { img ->
                    if (img.isNotEmpty()) {
                        imageViewContactPicture.visible

                        imageLoader.load(
                            imageViewContactPicture,
                            img,
                            ImageLoaderOptions.Builder()
                                .placeholderResId(R.drawable.ic_profile_avatar_circle)
                                .transformation(Transformation.CircleCrop)
                                .build()
                        )
                    }
                }
            }
        }
    }

    private fun setupNumberPad() {
        amountPadBinding.let { numberPad ->
            numberPad.button0.setOnClickListener { addAmountCharacter('0') }
            numberPad.button1.setOnClickListener { addAmountCharacter( '1') }
            numberPad.button2.setOnClickListener { addAmountCharacter( '2') }
            numberPad.button3.setOnClickListener { addAmountCharacter( '3') }
            numberPad.button4.setOnClickListener { addAmountCharacter( '4') }
            numberPad.button5.setOnClickListener { addAmountCharacter( '5') }
            numberPad.button6.setOnClickListener { addAmountCharacter( '6') }
            numberPad.button7.setOnClickListener { addAmountCharacter( '7') }
            numberPad.button8.setOnClickListener { addAmountCharacter( '8') }
            numberPad.button9.setOnClickListener { addAmountCharacter( '9') }
            numberPad.buttonBackspace.setOnClickListener { removeLastCharacter() }
        }
    }

    protected fun removeLastCharacter() {
        amountBinding.textViewAmount.text?.let { amountString ->
            amountString.toString().dropLast(1).let { updatedAmountString ->
                viewModel.updateAmount(updatedAmountString)
            }
        }
    }

    protected fun addAmountCharacter(c: Char) {
        amountBinding.apply {
            textViewAmount.text?.let { amountString ->
                val updatedAmountString = "$amountString$c"
                viewModel.updateAmount(updatedAmountString)
            }
        }
    }

    override fun subscribeToViewStateFlow() {
        super.subscribeToViewStateFlow()

        onStopSupervisor.scope.launch(viewModel.mainImmediate) {
            viewModel.amountViewStateContainer.collect { viewState ->
                @Exhaustive
                when (viewState) {
                    is AmountViewState.Idle -> {}

                    is AmountViewState.AmountUpdated -> {
                        amountBinding.textViewAmount.text = viewState.amountString
                        confirmationBinding.buttonConfirm.goneIfFalse(viewState.amountString.isNotEmpty())
                    }
                }
            }
        }
    }

    override suspend fun onSideEffectCollect(sideEffect: PaymentSideEffect) {
        sideEffect.execute(requireActivity())
    }
}

package chat.sphinx.payment_send.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.insetter_activity.InsetterActivity
import chat.sphinx.insetter_activity.addNavigationBarPadding
import chat.sphinx.payment_send.R
import chat.sphinx.payment_send.databinding.FragmentPaymentSendBinding
import chat.sphinx.wrapper_contact.Contact
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.base.BaseFragment
import kotlinx.coroutines.launch
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

@AndroidEntryPoint
internal class PaymentSendFragment: BaseFragment<
        PaymentSendViewState,
        PaymentSendViewModel,
        FragmentPaymentSendBinding
        >(R.layout.fragment_payment_send)
{

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    override val viewModel: PaymentSendViewModel by viewModels()
    override val binding: FragmentPaymentSendBinding by viewBinding(FragmentPaymentSendBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.includePaymentSendHeader.apply {
            textViewDetailScreenHeaderName.text = getString(R.string.payment_send_header_name)
            textViewDetailScreenClose.setOnClickListener {
                lifecycleScope.launch(viewModel.mainImmediate) {
                    viewModel.navigator.closeDetailScreen()
                }
            }
        }

        setupFooter()
    }

    private fun setupFooter() {
        val insetterActivity = (requireActivity() as InsetterActivity)
        insetterActivity.addNavigationBarPadding(binding.layoutConstraintPaymentSend)
    }

    private fun setupDestination(contact: Contact) {
        binding.apply {
            contact.alias?.value?.let { alias ->
                textViewContactName.text = alias
            }

            onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                contact.photoUrl?.value?.let { img ->
                    if (img.isNotEmpty()) {
                        imageLoader.load(
                            imageViewContactPicture,
                            img,
                            ImageLoaderOptions.Builder()
                                .placeholderResId(R.drawable.ic_profile_avatar_circle)
                                .transformation(Transformation.CircleCrop)
                                .build()
                        )
                    }
                } ?: imageViewContactPicture
                    .setImageDrawable(
                        ContextCompat.getDrawable(
                            binding.root.context,
                            R.drawable.ic_profile_avatar_circle
                        )
                    )
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: PaymentSendViewState) {
        @Exhaustive
        when (viewState) {
            is PaymentSendViewState.Idle -> {}

            is PaymentSendViewState.SendingChatPayment -> {
                setupDestination(viewState.contact)
            }
        }
    }
}

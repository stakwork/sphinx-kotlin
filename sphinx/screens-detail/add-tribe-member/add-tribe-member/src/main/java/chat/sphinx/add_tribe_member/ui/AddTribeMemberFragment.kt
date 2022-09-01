package chat.sphinx.add_tribe_member.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.viewModels
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.add_tribe_member.databinding.FragmentAddTribeMemberBinding
import chat.sphinx.add_tribe_member.R
import chat.sphinx.concept_image_loader.ImageLoader
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import javax.inject.Inject

@AndroidEntryPoint
internal class AddTribeMemberFragment: SideEffectFragment<
        Context,
        AddTribeMemberSideEffect,
        AddTribeMemberViewState,
        AddTribeMemberViewModel,
        FragmentAddTribeMemberBinding
        >(R.layout.fragment_add_tribe_member)
{
    override val viewModel: AddTribeMemberViewModel by viewModels()
    override val binding: FragmentAddTribeMemberBinding by viewBinding(FragmentAddTribeMemberBinding::bind)

    @Inject
    lateinit var imageLoader: ImageLoader<ImageView>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        binding.includeTribeMembersListHeader.apply {
//            textViewDetailScreenHeaderNavBack.visible
//            textViewDetailScreenHeaderName.text = getString(R.string.tribe_members_list_header)
//
//            textViewDetailScreenClose.gone
//
//            textViewDetailScreenHeaderNavBack.setOnClickListener {
//                lifecycleScope.launch(viewModel.mainImmediate) {
//                    viewModel.navigator.popBackStack()
//                }
//            }
//        }
    }

    override suspend fun onViewStateFlowCollect(viewState: AddTribeMemberViewState) {

    }

    override suspend fun onSideEffectCollect(sideEffect: AddTribeMemberSideEffect) {
        sideEffect.execute(requireActivity())
    }
}

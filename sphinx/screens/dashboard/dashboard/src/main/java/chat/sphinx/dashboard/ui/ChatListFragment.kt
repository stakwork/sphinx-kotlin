package chat.sphinx.dashboard.ui

import android.content.Context
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import app.cash.exhaustive.Exhaustive
import by.kirich1409.viewbindingdelegate.viewBinding
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_user_colors_helper.UserColorsHelper
import chat.sphinx.dashboard.R
import chat.sphinx.dashboard.databinding.FragmentChatListBinding
import chat.sphinx.dashboard.ui.adapter.ChatListAdapter
import chat.sphinx.dashboard.ui.adapter.ChatListFooterAdapter
import chat.sphinx.dashboard.ui.adapter.DashboardFooterAdapter
import chat.sphinx.dashboard.ui.viewstates.ChatFilter
import chat.sphinx.dashboard.ui.viewstates.ChatListFooterButtonsViewState
import chat.sphinx.dashboard.ui.viewstates.ChatListViewState
import chat.sphinx.dashboard.ui.viewstates.ChatViewState
import chat.sphinx.resources.SphinxToastUtils
import chat.sphinx.resources.inputMethodManager
import chat.sphinx.wrapper_chat.ChatType
import dagger.hilt.android.AndroidEntryPoint
import io.matthewnelson.android_feature_screens.navigation.CloseAppOnBackPress
import io.matthewnelson.android_feature_screens.ui.sideeffect.SideEffectFragment
import io.matthewnelson.android_feature_screens.util.gone
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_screens.util.visible
import io.matthewnelson.concept_views.viewstate.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("NOTHING_TO_INLINE")
private inline fun FragmentChatListBinding.searchBarClearFocus() {
    layoutSearchBar.editTextDashboardSearch.clearFocus()
}

@AndroidEntryPoint
internal class ChatListFragment : SideEffectFragment<
        Context,
        ChatListSideEffect,
        ChatListViewState,
        ChatListViewModel,
        FragmentChatListBinding
        >(R.layout.fragment_chat_list)
{
    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var imageLoader: ImageLoader<ImageView>

    @Inject
    @Suppress("ProtectedInFinal")
    protected lateinit var userColorsHelper: UserColorsHelper

    override val viewModel: ChatListViewModel by viewModels()
    override val binding: FragmentChatListBinding by viewBinding(FragmentChatListBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSearch()
        setupChats()
    }

    override fun onResume() {
        super.onResume()

        BackPressHandler(binding.root.context)
            .enableDoubleTapToClose(viewLifecycleOwner, SphinxToastUtils())
            .addCallback(viewLifecycleOwner, requireActivity())
    }

    private inner class BackPressHandler(context: Context): CloseAppOnBackPress(context) {
        override fun handleOnBackPressed() {
            if (
                parentFragment is DashboardFragment &&
                (parentFragment as DashboardFragment)?.closeDrawerIfOpen() == true
            ) {
                return
            } else {
                binding.searchBarClearFocus()
                super.handleOnBackPressed()
            }
        }
    }

    private fun setupChats() {
        binding.layoutChatListChats.recyclerViewChats.apply {
            val linearLayoutManager = LinearLayoutManager(context)
            val chatListAdapter = ChatListAdapter(
                this,
                linearLayoutManager,
                imageLoader,
                viewLifecycleOwner,
                onStopSupervisor,
                viewModel,
                userColorsHelper
            )

            val chatListFooterAdapter = ChatListFooterAdapter(viewLifecycleOwner, onStopSupervisor, viewModel)
            val footerSpaceAdapter = DashboardFooterAdapter()
            this.setHasFixedSize(false)
            layoutManager = linearLayoutManager

            adapter = ConcatAdapter(
                chatListAdapter,
                chatListFooterAdapter,
                footerSpaceAdapter
            )

            itemAnimator = null

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    if (parentFragment is DashboardFragment) {
                        val bottomOfScroll = !canScrollVertically(1)
                        val topOfScroll = !canScrollVertically(-1)
                        val scrollNotAvailable = (bottomOfScroll && topOfScroll)
                        (parentFragment as DashboardFragment)?.shouldToggleNavBar(
                            (dy <= 0 && !bottomOfScroll) || scrollNotAvailable
                        )
                    }
                }
            })
        }
    }

    private fun setupSearch() {
        binding.layoutSearchBar.apply {
            editTextDashboardSearch.addTextChangedListener { editable ->
                buttonDashboardSearchClear.goneIfFalse(editable.toString().isNotEmpty())

                onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                    viewModel.updateChatListFilter(
                        if (editable.toString().isNotEmpty()) {
                            ChatFilter.FilterBy(editable.toString())
                        } else {
                            ChatFilter.ClearFilter
                        }
                    )
                }
            }

            includeLayoutButtonAddTribe.root.setOnClickListener {
                onStopSupervisor.scope.launch(viewModel.mainImmediate) {
                    viewModel.toTribesDiscover()
                }
            }

            editTextDashboardSearch.setOnEditorActionListener(object: TextView.OnEditorActionListener {
                override fun onEditorAction(v: TextView, actionId: Int, event: KeyEvent?): Boolean {
                    if (actionId == EditorInfo.IME_ACTION_DONE || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                        editTextDashboardSearch.let { editText ->
                            binding.root.context.inputMethodManager?.let { imm ->
                                if (imm.isActive(editText)) {
                                    imm.hideSoftInputFromWindow(editText.windowToken, 0)
                                    editText.clearFocus()
                                }
                            }
                        }
                        return true
                    }
                    return false
                }
            })

            buttonDashboardSearchClear.setOnClickListener {
                editTextDashboardSearch.setText("")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        binding.searchBarClearFocus()
    }

    override suspend fun onSideEffectCollect(sideEffect: ChatListSideEffect) {
        sideEffect.execute(binding.root.context)
    }

    companion object {
        fun newInstance(
            updateBackgroundLoginTime: Boolean = false,
            chatListType: ChatType = ChatType.Conversation,
            deepLink: String? = null,
        ): ChatListFragment {
            return ChatListFragment().apply {
                val args = ChatListFragmentArgs.Builder(updateBackgroundLoginTime, chatListType.value)
                args.argDeepLink = deepLink

                arguments = args.build().toBundle()
            }
        }
    }

    override suspend fun onViewStateFlowCollect(viewState: ChatListViewState) {
        viewModel.chatListFooterButtonsViewStateContainer.collect { viewState ->
            binding.layoutSearchBar.includeLayoutButtonAddTribe.apply {
                @Exhaustive
                when (viewState) {
                    is ChatListFooterButtonsViewState.Idle -> {
                        root.gone
                    }
                    is ChatListFooterButtonsViewState.ButtonsVisibility -> {
                        root.goneIfFalse(viewState.discoverTribesVisible)
                    }
                }
            }
        }
    }

    override fun subscribeToViewStateFlow() {
        onStopSupervisor.scope.launch(viewModel.mainImmediate) {

            val chatViewStateFlow = flow {
                viewModel.chatViewStateContainer.collect { emit(it) }
            }

            chatViewStateFlow.combine(viewModel.hasSingleContact) { chatViewState, isSingleContact ->
                Pair(chatViewState, isSingleContact)
            }.collect { (chatViewState, isSingleContact) ->
                when {
                    chatViewState.list.isEmpty() -> {
                        binding.progressBarChatList.visible

                        if (isSingleContact == true) {
                            binding.progressBarChatList.gone
//                            binding.welcomeToSphinx.visible
                        }
                    }
                    else -> {
                        binding.progressBarChatList.gone
                        binding.welcomeToSphinx.gone
                    }
                }
            }
        }

        super.subscribeToViewStateFlow()
    }
}

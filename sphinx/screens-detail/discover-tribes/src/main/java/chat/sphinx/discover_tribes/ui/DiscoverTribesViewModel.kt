package chat.sphinx.discover_tribes.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_network_query_chat.model.TribeDto
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.discover_tribes.model.DiscoverTribesTag
import chat.sphinx.discover_tribes.navigation.DiscoverTribesNavigator
import chat.sphinx.discover_tribes.viewstate.DiscoverTribesLoadingViewState
import chat.sphinx.discover_tribes.viewstate.DiscoverTribesTagsViewState
import chat.sphinx.discover_tribes.viewstate.DiscoverTribesViewState
import chat.sphinx.wrapper_common.tribe.TribeJoinLink
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoverTribesViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: DiscoverTribesNavigator,
    private val chatRepository: ChatRepository
    ): SideEffectViewModel<
        Context,
        DiscoverTribesSideEffect,
        DiscoverTribesViewState,
        >(dispatchers, DiscoverTribesViewState.Idle)
{
    val discoverTribesTagsViewStateContainer: ViewStateContainer<DiscoverTribesTagsViewState> by lazy {
        ViewStateContainer(DiscoverTribesTagsViewState.Closed)
    }

    val discoverTribesLoadingViewStateContainer: ViewStateContainer<DiscoverTribesLoadingViewState> by lazy {
        ViewStateContainer(DiscoverTribesLoadingViewState.Open)
    }

    var page = 1

    private val _tribeTagsStateFlow: MutableStateFlow<Array<DiscoverTribesTag>> by lazy {
        MutableStateFlow(
            arrayOf(
                DiscoverTribesTag("NSFW", false),
                DiscoverTribesTag("Bitcoin", false),
                DiscoverTribesTag("Lightning", false),
                DiscoverTribesTag("Sphinx", false),
                DiscoverTribesTag("Podcast", false),
                DiscoverTribesTag("Crypto", false),
                DiscoverTribesTag("Music", false),
                DiscoverTribesTag("Tech", false),
                DiscoverTribesTag("Altcoins", false)
            )
        )
    }

    val tribeTagsStateFlow: StateFlow<Array<DiscoverTribesTag>>
        get() = _tribeTagsStateFlow.asStateFlow()

    fun changeSelectTag(position: Int) {
        _tribeTagsStateFlow.value[position].isSelected = !_tribeTagsStateFlow.value[position].isSelected
    }

    private val _tribeSelectedTagsList: MutableStateFlow<List<String>?> by lazy {
        MutableStateFlow(null)
    }

    val tribeSelectedTagsList: StateFlow<List<String>?>
        get() = _tribeSelectedTagsList.asStateFlow()

    fun getTribeSelectedTags() {
        _tribeSelectedTagsList.value = tribeTagsStateFlow.value.filter { it.isSelected }.map { it.name }
    }

    private val _discoverTribeStateFlow: MutableStateFlow<List<TribeDto>> by lazy {
        MutableStateFlow(listOf())
    }

    val discoverTribesStateFlow: StateFlow<List<TribeDto>>
        get() = _discoverTribeStateFlow.asStateFlow()


    private var discoverTribeJob: Job? = null
    fun getDiscoverTribesList(searchTerm: String? = null) {
        if (discoverTribeJob?.isActive == true) {
            return
        }

        discoverTribeJob = viewModelScope.launch(mainImmediate) {

            val tags: String? = tribeSelectedTagsList.value?.toString()?.replace(
                "[", "")?.
            replace("]", "")?.replace("\\s".toRegex(), "")

            chatRepository.getAllDiscoverTribes(
                page,
                searchTerm,
                tags
            ).collect { discoverTribes ->
                _discoverTribeStateFlow.value = _discoverTribeStateFlow.value + discoverTribes
                discoverTribesLoadingViewStateContainer.updateViewState(
                    DiscoverTribesLoadingViewState.Closed
                )
            }
        }
    }

    fun cleanDiscoverTribesList() {
        _discoverTribeStateFlow.value = listOf()
    }
    fun cleanDiscoverTribesTags() {
        _tribeSelectedTagsList.value = listOf()
    }

    fun handleTribeLink(uuid: String) {
        val tribeLink = TribeJoinLink("sphinx.chat://?action=tribe&uuid=${uuid}&host=tribes.sphinx.chat")

        viewModelScope.launch(mainImmediate) {
            navigator.toJoinTribeDetail(tribeLink)
        }
    }
}
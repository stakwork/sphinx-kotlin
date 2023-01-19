package chat.sphinx.discover_tribes.ui

import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.discover_tribes.model.DiscoverTribesTag
import chat.sphinx.discover_tribes.navigation.DiscoverTribesNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.BaseViewModel
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class DiscoverTribesViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val navigator: DiscoverTribesNavigator,
    private val chatRepository: ChatRepository,
    ): BaseViewModel<DiscoverTribesViewState>(dispatchers, DiscoverTribesViewState.Idle)
{
    val discoverTribesTagsViewStateContainer: ViewStateContainer<DiscoverTribesTagsViewState> by lazy {
        ViewStateContainer(DiscoverTribesTagsViewState.Closed)
    }

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

    suspend fun getAllDiscoverTribes() {
        viewModelScope.launch(mainImmediate) {
            chatRepository.getAllDiscoverTribes().collect { discoverTribes ->
            }
        }
    }
}
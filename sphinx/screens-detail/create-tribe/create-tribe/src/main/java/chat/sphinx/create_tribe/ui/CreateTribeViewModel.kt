package chat.sphinx.create_tribe.ui

import android.content.Context
import androidx.lifecycle.viewModelScope
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_chat.model.CreateTribe
import chat.sphinx.create_tribe.navigation.CreateTribeNavigator
import chat.sphinx.kotlin_response.Response
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class CreateTribeViewModel @Inject constructor(
    dispatchers: CoroutineDispatchers,
    val chatRepository: ChatRepository,
    val navigator: CreateTribeNavigator,
): SideEffectViewModel<
        Context,
        CreateTribeSideEffect,
        CreateTribeViewState>(dispatchers, CreateTribeViewState.Idle)
{
    private fun showNameAndDescriptionRequired() {
        viewModelScope.launch(mainImmediate) {
            submitSideEffect(CreateTribeSideEffect.NameAndDescriptionRequired)
        }
    }

    fun createTribe(createTribeBuilder: CreateTribe.Builder) {
        if (createTribeBuilder.hasRequiredFields) {
            createTribeBuilder.build()?.let {
                viewModelScope.launch(mainImmediate) {
                    when(chatRepository.createTribe(it)) {
                        is Response.Error -> {
                            submitSideEffect(CreateTribeSideEffect.FailedToCreateTribe)
                        }
                        is Response.Success -> {

                            navigator.closeDetailScreen()
                        }
                    }
                }

            }
        } else {
            showNameAndDescriptionRequired()
        }
    }
}

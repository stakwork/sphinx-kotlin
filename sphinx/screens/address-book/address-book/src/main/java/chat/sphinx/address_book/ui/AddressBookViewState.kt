package chat.sphinx.address_book.ui

import chat.sphinx.wrapper_contact.Contact
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewState
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.annotation.meta.Exhaustive

internal sealed class AddressBookViewState: ViewState<AddressBookViewState>() {

    abstract val list: List<Contact>
    abstract val originalList: List<Contact>

    class ListMode(
        override val list: List<Contact>,
        override val originalList: List<Contact>
    ): AddressBookViewState()

    class SearchMode(
        val filter: AddressBookFilter.FilterBy,
        override val list: List<Contact>,
        override val originalList: List<Contact>
    ): AddressBookViewState()
}

internal sealed class AddressBookFilter {

    /**
     * Will use the current filter (if any) applied to the list of [Contact]s.
     * */
    object UseCurrent: AddressBookFilter()

    /**
     * Will filter the list of [Contact]s based on the provided [value]
     * */
    class FilterBy(val value: CharSequence): AddressBookFilter() {
        init {
            require(value.isNotEmpty()) {
                "ChatFilter.FilterBy cannot be empty. Use ClearFilter."
            }
        }
    }

    /**
     * Clears any applied filters.
     * */
    object ClearFilter: AddressBookFilter()
}

@Suppress("NOTHING_TO_INLINE")
private inline fun List<Contact>.filterAddressBookContacts(
    filter: CharSequence
): List<Contact> =
    filter {
        it.alias?.value?.contains(filter, ignoreCase = true) == true
    }

// TODO: Need to preserve the original list when going between list and search modes.
internal class AddressBookViewStateContainer(
    private val dispatchers: CoroutineDispatchers,
): ViewStateContainer<AddressBookViewState>(AddressBookViewState.ListMode(emptyList(), emptyList())) {

    override fun updateViewState(viewState: AddressBookViewState) {
        throw IllegalStateException("Must utilize updateAddressBookContacts method")
    }

    private val lock = Mutex()

    /**
     * Sorts and filters the provided list.
     *
     * @param [addressBookContacts] if `null` uses the current, already sorted list.
     * @param [filter] the type of filtering to apply to the list. See [AddressBookFilter].
     * */
    suspend fun updateAddressBookContacts(
        addressBookContacts: List<Contact>?,
        filter: AddressBookFilter = AddressBookFilter.UseCurrent
    ) {
        lock.withLock {
            val sortedAddressBookContacts = if (addressBookContacts != null) {
                withContext(dispatchers.default) {
                    addressBookContacts.sortedBy { it.alias?.value }
                }
            } else {
                viewStateFlow.value.originalList
            }

            @Exhaustive
            when (filter) {
                is AddressBookFilter.UseCurrent -> {

                    @Exhaustive
                    when (val viewState = viewStateFlow.value) {
                        is AddressBookViewState.ListMode -> {
                            super.updateViewState(
                                AddressBookViewState.ListMode(
                                    sortedAddressBookContacts,
                                    sortedAddressBookContacts
                                )
                            )
                        }
                        is AddressBookViewState.SearchMode -> {
                            super.updateViewState(
                                AddressBookViewState.SearchMode(
                                    viewState.filter,
                                    withContext(dispatchers.default) {
                                        sortedAddressBookContacts
                                            .filterAddressBookContacts(viewState.filter.value)
                                    },
                                    sortedAddressBookContacts
                                )
                            )
                        }
                    }

                }
                is AddressBookFilter.ClearFilter -> {
                    super.updateViewState(
                        AddressBookViewState.ListMode(
                            sortedAddressBookContacts,
                            sortedAddressBookContacts
                        )
                    )
                }
                is AddressBookFilter.FilterBy -> {
                    super.updateViewState(
                        AddressBookViewState.SearchMode(
                            filter,
                            withContext(dispatchers.default) {
                                sortedAddressBookContacts
                                    .filterAddressBookContacts(filter.value)
                            },
                            sortedAddressBookContacts
                        )
                    )
                }
            }
        }
    }
}

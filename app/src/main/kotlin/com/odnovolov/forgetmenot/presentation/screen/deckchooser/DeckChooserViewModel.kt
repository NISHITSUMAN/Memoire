package com.odnovolov.forgetmenot.presentation.screen.deckchooser

import com.odnovolov.forgetmenot.domain.architecturecomponents.CopyableCollection
import com.odnovolov.forgetmenot.domain.architecturecomponents.share
import com.odnovolov.forgetmenot.domain.entity.Card
import com.odnovolov.forgetmenot.domain.entity.Deck
import com.odnovolov.forgetmenot.domain.entity.DeckList
import com.odnovolov.forgetmenot.domain.entity.GlobalState
import com.odnovolov.forgetmenot.domain.isCardAvailableForExercise
import com.odnovolov.forgetmenot.presentation.screen.deckchooser.DeckChooserScreenState.Purpose.*
import com.odnovolov.forgetmenot.presentation.screen.home.*
import com.odnovolov.forgetmenot.presentation.screen.home.DeckListItem.*
import com.odnovolov.forgetmenot.presentation.screen.home.HomeViewModel.RawDeckPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

class DeckChooserViewModel(
    private val screenState: DeckChooserScreenState,
    globalState: GlobalState,
    deckReviewPreference: DeckReviewPreference
) {
    val purpose: DeckChooserScreenState.Purpose get() = screenState.purpose

    private val searchText: Flow<String> = screenState.flowOf(DeckChooserScreenState::searchText)

    private val hasSearchText: Flow<Boolean> = searchText.map { it.isNotEmpty() }
        .distinctUntilChanged()

    val currentDeckList: Flow<DeckList?> =
        deckReviewPreference.flowOf(DeckReviewPreference::deckList)

    val selectableDeckLists: Flow<List<SelectableDeckList>> = combine(
        globalState.flowOf(GlobalState::decks),
        globalState.flowOf(GlobalState::deckLists),
        deckReviewPreference.flowOf(DeckReviewPreference::deckList)
    ) { decks: CopyableCollection<Deck>,
        deckLists: CopyableCollection<DeckList>,
        currentDeckList: DeckList?
        ->
        val selectableDeckLists = ArrayList<SelectableDeckList>()
        val allDecksDeckList = SelectableDeckList(
            id = null,
            name = null,
            color = DeckReviewPreference.DEFAULT_DECK_LIST_COLOR,
            size = decks.size,
            isSelected = currentDeckList == null
        )
        selectableDeckLists.add(allDecksDeckList)
        val createdDeckLists = deckLists
            .sortedBy { deckList: DeckList -> deckList.name }
            .map { deckList: DeckList ->
                SelectableDeckList(
                    deckList.id,
                    deckList.name,
                    deckList.color,
                    deckList.deckIds.size,
                    isSelected = deckList.id == currentDeckList?.id
                )
            }
        selectableDeckLists.addAll(createdDeckLists)
        selectableDeckLists
    }

    private val rawDecksPreview: Flow<List<RawDeckPreview>> = combine(
        globalState.flowOf(GlobalState::decks),
        currentDeckList,
        hasSearchText
    ) { decks: Collection<Deck>, currentDeckList: DeckList?, hasSearchText: Boolean ->
        if (hasSearchText || currentDeckList == null) {
            decks
        } else {
            decks.filter { deck: Deck ->
                deck.id in currentDeckList.deckIds
            }
        }
    }
        .combine(globalState.flowOf(GlobalState::deckLists)) { decks: Collection<Deck>,
                                                               deckLists: Collection<DeckList> ->
            decks.map { deck: Deck ->
                val averageLaps: Double = deck.cards
                    .map { it.lap }
                    .average()
                val learnedCount = deck.cards.count { it.isLearned }
                val numberOfCardsReadyForExercise =
                    if (deck.exercisePreference.intervalScheme == null) {
                        null
                    } else {
                        deck.cards.count { card: Card ->
                            isCardAvailableForExercise(card, deck.exercisePreference.intervalScheme)
                        }
                    }
                val deckListColors: List<Int> = deckLists.mapNotNull { deckList: DeckList ->
                    if (deck.id in deckList.deckIds) deckList.color else null
                }
                RawDeckPreview(
                    deckId = deck.id,
                    deckName = deck.name,
                    createdAt = deck.createdAt,
                    averageLaps = averageLaps,
                    learnedCount = learnedCount,
                    totalCount = deck.cards.size,
                    numberOfCardsReadyForExercise = numberOfCardsReadyForExercise,
                    lastTestedAt = deck.lastTestedAt,
                    isPinned = deck.isPinned,
                    deckListColors = deckListColors
                )
            }
        }
        .share()

    val deckSorting: Flow<DeckSorting> =
        deckReviewPreference.flowOf(DeckReviewPreference::deckSorting)

    private val sortedDecksPreview: Flow<List<RawDeckPreview>> = combine(
        rawDecksPreview,
        deckSorting
    ) { rawDecksPreview: List<RawDeckPreview>, deckSorting: DeckSorting ->
        val comparator = DeckPreviewComparator(deckSorting)
        rawDecksPreview.sortedWith(comparator)
    }
        .share()

    private val decksPreview: Flow<List<DeckPreview>> = combine(
        sortedDecksPreview,
        searchText
    ) { sortedDecksPreview: List<RawDeckPreview>,
        searchText: String
        ->
        if (searchText.isEmpty()) {
            sortedDecksPreview
                .map { rawDeckPreview: RawDeckPreview ->
                    rawDeckPreview.toDeckPreview(searchMatchingRanges = null)
                }
        } else {
            sortedDecksPreview
                .mapNotNull { rawDeckPreview: RawDeckPreview ->
                    val searchMatchingRanges: List<IntRange> =
                        findMatchingRange(rawDeckPreview.deckName, searchText)
                            ?: return@mapNotNull null
                    rawDeckPreview.toDeckPreview(searchMatchingRanges)
                }
        }
    }
        .share()
        .flowOn(Dispatchers.Default)

    private fun findMatchingRange(source: String, search: String): List<IntRange>? {
        if (search.isEmpty()) return null
        var start = source.indexOf(search, ignoreCase = true)
        if (start < 0) return null
        val result = ArrayList<IntRange>()
        while (start >= 0) {
            val end = start + search.length
            result += start..end
            start = source.indexOf(search, startIndex = end, ignoreCase = true)
        }
        return result
    }

    val deckListItems: Flow<List<DeckListItem>> = combine(
        decksPreview,
        hasSearchText
    ) { decksPreview: List<DeckPreview>,
        hasSearchText: Boolean
        ->
        if (hasSearchText) {
            decksPreview
        } else {
            listOf(Header) + decksPreview
        }
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    val decksNotFound: Flow<Boolean> = combine(
        hasSearchText,
        decksPreview
    ) { hasSearchText: Boolean, decksPreview: List<DeckPreview> ->
        hasSearchText && decksPreview.isEmpty()
    }
        .distinctUntilChanged()

    val isAddDeckButtonVisible: Boolean
        get() = when (screenState.purpose) {
            ToImportCards -> false
            else -> true
        }
}
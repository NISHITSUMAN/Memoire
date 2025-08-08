package com.odnovolov.forgetmenot.presentation.screen.deckeditor

import com.odnovolov.forgetmenot.domain.entity.Deck
import com.odnovolov.forgetmenot.domain.interactor.cardeditor.BatchCardEditor
import com.odnovolov.forgetmenot.domain.interactor.cardeditor.EditableCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DeckEditorViewModel(
    private val screenState: DeckEditorScreenState,
    private val batchCardEditorState: BatchCardEditor.State
) {
    val tabs: DeckEditorTabs get() = screenState.tabs

    val deckName: Flow<String> = screenState.deck.flowOf(Deck::name)

    val isSelectionMode: Flow<Boolean> =
        batchCardEditorState.flowOf(BatchCardEditor.State::selectedCards)
            .map { editableCards: Collection<EditableCard> -> editableCards.isNotEmpty() }

    val numberOfSelectedCards: Flow<Int> =
        batchCardEditorState.flowOf(BatchCardEditor.State::selectedCards)
            .map { editableCards: Collection<EditableCard> -> editableCards.size }

    val isMarkAsLearnedOptionAvailable: Boolean
        get() = batchCardEditorState.selectedCards.any { editableCard: EditableCard ->
            !editableCard.card.isLearned
        }

    val isMarkAsUnlearnedOptionAvailable: Boolean
        get() = batchCardEditorState.selectedCards.any { editableCard: EditableCard ->
            editableCard.card.isLearned
        }
}
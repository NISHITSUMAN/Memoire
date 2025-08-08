package com.odnovolov.forgetmenot.presentation.screen.deckeditor

import com.odnovolov.forgetmenot.domain.entity.*
import com.odnovolov.forgetmenot.domain.generateId
import com.odnovolov.forgetmenot.domain.interactor.cardeditor.BatchCardEditor
import com.odnovolov.forgetmenot.domain.interactor.cardeditor.CardsEditor.State
import com.odnovolov.forgetmenot.domain.interactor.cardeditor.CardsEditorForEditingDeck
import com.odnovolov.forgetmenot.domain.interactor.cardeditor.EditableCard
import com.odnovolov.forgetmenot.presentation.common.LongTermStateSaver
import com.odnovolov.forgetmenot.presentation.common.Navigator
import com.odnovolov.forgetmenot.presentation.common.ShortTermStateProvider
import com.odnovolov.forgetmenot.presentation.common.base.BaseController
import com.odnovolov.forgetmenot.presentation.screen.cardseditor.CardsEditorDiScope
import com.odnovolov.forgetmenot.presentation.screen.changegrade.ChangeGradeCaller
import com.odnovolov.forgetmenot.presentation.screen.changegrade.ChangeGradeDiScope
import com.odnovolov.forgetmenot.presentation.screen.changegrade.ChangeGradeDialogState
import com.odnovolov.forgetmenot.presentation.screen.changegrade.GradeItem
import com.odnovolov.forgetmenot.presentation.screen.deckchooser.DeckChooserDiScope
import com.odnovolov.forgetmenot.presentation.screen.deckchooser.DeckChooserScreenState
import com.odnovolov.forgetmenot.presentation.screen.deckchooser.DeckChooserScreenState.Purpose.ToCopyCardsInDeckEditor
import com.odnovolov.forgetmenot.presentation.screen.deckchooser.DeckChooserScreenState.Purpose.ToMoveCardsInDeckEditor
import com.odnovolov.forgetmenot.presentation.screen.deckeditor.DeckEditorController.Command
import com.odnovolov.forgetmenot.presentation.screen.deckeditor.DeckEditorController.Command.*
import com.odnovolov.forgetmenot.presentation.screen.deckeditor.DeckEditorEvent.*
import com.odnovolov.forgetmenot.presentation.screen.renamedeck.RenameDeckDiScope
import com.odnovolov.forgetmenot.presentation.screen.renamedeck.RenameDeckDialogPurpose.ToRenameExistingDeck
import com.odnovolov.forgetmenot.presentation.screen.renamedeck.RenameDeckDialogState

class DeckEditorController(
    private val batchCardEditor: BatchCardEditor,
    private val screenState: DeckEditorScreenState,
    private val navigator: Navigator,
    private val globalState: GlobalState,
    private val longTermStateSaver: LongTermStateSaver,
    private val batchCardEditorProvider: ShortTermStateProvider<BatchCardEditor>
) : BaseController<DeckEditorEvent, Command>() {
    sealed class Command {
        class ShowCardsAreInvertedMessage(val numberOfInvertedCards: Int) : Command()
        class ShowGradeIsChangedMessage(val grade: Int, val numberOfAffectedCards: Int) : Command()
        class ShowCardsAreMarkedAsLearnedMessage(val numberOfMarkedCards: Int) : Command()
        class ShowCardsAreMarkedAsUnlearnedMessage(val numberOfMarkedCards: Int) : Command()
        class ShowCardsAreRemovedMessage(val numberOfRemovedCards: Int) : Command()
        class ShowCardsAreMovedMessage(
            val numberOfMovedCards: Int,
            val deckNameToWhichCardsWereMoved: String
        ) : Command()

        class ShowCardsAreCopiedMessage(
            val numberOfCopiedCards: Int,
            val deckNameToWhichCardsWereCopied: String
        ) : Command()
    }

    override fun handle(event: DeckEditorEvent) {
        when (event) {
            RenameDeckButtonClicked -> {
                navigator.showRenameDeckDialogFromDeckEditor {
                    val deck = screenState.deck
                    val dialogState = RenameDeckDialogState(
                        purpose = ToRenameExistingDeck(deck),
                        typedDeckName = deck.name
                    )
                    RenameDeckDiScope.create(dialogState)
                }
            }

            AddCardButtonClicked -> {
                navigator.navigateToCardsEditorFromDeckEditor {
                    val deck = screenState.deck
                    val editableCards: List<EditableCard> =
                        deck.cards.map { card -> EditableCard(card, deck) }
                            .plus(EditableCard(Card(generateId(), "", ""), deck))
                    val position: Int = editableCards.lastIndex
                    val cardsEditorState = State(editableCards, position)
                    val cardsEditor = CardsEditorForEditingDeck(
                        deck,
                        isNewDeck = false,
                        cardsEditorState,
                        globalState
                    )
                    CardsEditorDiScope.create(cardsEditor)
                }
            }

            CancelledCardSelection -> {
                batchCardEditor.clearSelection()
            }

            SelectAllCardsButtonClicked -> {
                val deck: Deck = screenState.deck
                val allEditableCards: List<EditableCard> =
                    deck.cards.map { card: Card -> EditableCard(card, deck) }
                batchCardEditor.addCardsToSelection(allEditableCards)
            }

            InvertCardSelectionOptionWasSelected -> {
                val numberOfInvertedCards: Int = batchCardEditor.state.selectedCards.size
                batchCardEditor.invert()
                sendCommand(ShowCardsAreInvertedMessage(numberOfInvertedCards))
            }

            ChangeGradeCardSelectionOptionWasSelected -> {
                val gradeItems: List<GradeItem> =
                    screenState.deck.exercisePreference.intervalScheme
                        ?.intervals?.map { interval: Interval ->
                            GradeItem(
                                grade = interval.grade,
                                waitingPeriod = interval.value
                            )
                        }
                        ?: listOf(
                            GradeItem(0, null),
                            GradeItem(1, null),
                            GradeItem(2, null),
                            GradeItem(3, null),
                            GradeItem(4, null),
                            GradeItem(5, null),
                            GradeItem(6, null)
                        )
                navigator.showChangeGradeDialogFromDeckEditor {
                    val dialogState = ChangeGradeDialogState(
                        gradeItems,
                        caller = ChangeGradeCaller.DeckEditor
                    )
                    ChangeGradeDiScope.create(dialogState)
                }
            }

            is GradeWasSelected -> {
                val numberOfAffectedCards: Int = batchCardEditor.state.selectedCards.size
                batchCardEditor.changeGrade(event.grade)
                sendCommand(ShowGradeIsChangedMessage(event.grade, numberOfAffectedCards))
            }

            MarkAsLearnedCardSelectionOptionWasSelected -> {
                val numberOfMarkedCards: Int = batchCardEditor.state.selectedCards.size
                batchCardEditor.markAsLearned()
                sendCommand(ShowCardsAreMarkedAsLearnedMessage(numberOfMarkedCards))
            }

            MarkAsUnlearnedCardSelectionOptionWasSelected -> {
                val numberOfMarkedCards: Int = batchCardEditor.state.selectedCards.size
                batchCardEditor.markAsUnlearned()
                sendCommand(ShowCardsAreMarkedAsUnlearnedMessage(numberOfMarkedCards))
            }

            RemoveCardsCardSelectionOptionWasSelected -> {
                val numberOfRemovedCards: Int = batchCardEditor.state.selectedCards.size
                batchCardEditor.remove()
                sendCommand(ShowCardsAreRemovedMessage(numberOfRemovedCards))
            }

            MoveCardSelectionOptionWasSelected -> {
                navigator.navigateToDeckChooserFromDeckEditor {
                    val screenState = DeckChooserScreenState(purpose = ToMoveCardsInDeckEditor)
                    DeckChooserDiScope.create(screenState)
                }
            }

            is DeckToMoveCardsToWasSelected -> {
                val numberOfMovedCards: Int = batchCardEditor.state.selectedCards.size
                batchCardEditor.moveTo(event.abstractDeck)
                val deckName: String = event.abstractDeck.name
                sendCommand(
                    command = ShowCardsAreMovedMessage(numberOfMovedCards, deckName),
                    postponeIfNotActive = true
                )
            }

            CopyCardSelectionOptionWasSelected -> {
                navigator.navigateToDeckChooserFromDeckEditor {
                    val screenState = DeckChooserScreenState(purpose = ToCopyCardsInDeckEditor)
                    DeckChooserDiScope.create(screenState)
                }
            }

            is DeckToCopyCardsToWasSelected -> {
                val numberOfCopiedCards: Int = batchCardEditor.state.selectedCards.size
                batchCardEditor.copyTo(event.abstractDeck)
                val deckName: String = event.abstractDeck.name
                sendCommand(
                    command = ShowCardsAreCopiedMessage(numberOfCopiedCards, deckName),
                    postponeIfNotActive = true
                )
            }

            CancelSnackbarButtonClicked -> {
                batchCardEditor.cancelLastAction()
            }
        }
    }

    override fun saveState() {
        longTermStateSaver.saveStateByRegistry()
        batchCardEditorProvider.save(batchCardEditor)
    }
}
package com.odnovolov.forgetmenot.presentation.screen.cardfilterforautoplay

import com.odnovolov.forgetmenot.domain.interactor.autoplay.CardFilterForAutoplay
import com.odnovolov.forgetmenot.domain.interactor.autoplay.Player
import com.odnovolov.forgetmenot.domain.interactor.autoplay.PlayerStateCreator
import com.odnovolov.forgetmenot.domain.toDateTimeSpan
import com.odnovolov.forgetmenot.presentation.common.LongTermStateSaver
import com.odnovolov.forgetmenot.presentation.common.Navigator
import com.odnovolov.forgetmenot.presentation.common.ShortTermStateProvider
import com.odnovolov.forgetmenot.presentation.common.base.BaseController
import com.odnovolov.forgetmenot.presentation.screen.intervals.DisplayedInterval
import com.odnovolov.forgetmenot.presentation.screen.player.PlayerDiScope
import com.odnovolov.forgetmenot.presentation.screen.cardfilterforautoplay.CardFilterForAutoplayController.Command
import com.odnovolov.forgetmenot.presentation.screen.cardfilterforautoplay.CardFilterForAutoplayController.Command.ShowNoCardIsReadyForAutoplay
import com.odnovolov.forgetmenot.presentation.screen.cardfilterforautoplay.CardFilterForAutoplayEvent.*
import com.odnovolov.forgetmenot.presentation.screen.lasttested.LastTestedFilterDiScope
import com.odnovolov.forgetmenot.presentation.screen.lasttested.LastTestedFilterDialogCaller
import com.odnovolov.forgetmenot.presentation.screen.lasttested.LastTestedFilterDialogState
import com.soywiz.klock.DateTimeSpan
import com.soywiz.klock.days

class CardFilterForAutoplayController(
    private val playerStateCreator: PlayerStateCreator,
    private val cardFilter: CardFilterForAutoplay,
    private val navigator: Navigator,
    private val longTermStateSaver: LongTermStateSaver,
    private val playerCreatorStateProvider: ShortTermStateProvider<PlayerStateCreator.State>
) : BaseController<CardFilterForAutoplayEvent, Command>() {
    sealed class Command {
        object ShowNoCardIsReadyForAutoplay : Command()
    }

    override fun handle(event: CardFilterForAutoplayEvent) {
        when (event) {
            AvailableForExerciseCheckboxClicked -> {
                cardFilter.areCardsAvailableForExerciseIncluded =
                    !cardFilter.areCardsAvailableForExerciseIncluded
            }

            AwaitingCheckboxClicked -> {
                cardFilter.areAwaitingCardsIncluded = !cardFilter.areAwaitingCardsIncluded
            }

            LearnedCheckboxClicked -> {
                cardFilter.areLearnedCardsIncluded = !cardFilter.areLearnedCardsIncluded
            }

            is GradeRangeChanged -> {
                cardFilter.gradeRange = event.gradeRange
            }

            LastTestedFromButtonClicked -> {
                showLastTestedFilterDialog(isFromDialog = true)
            }

            LastTestedToButtonClicked -> {
                showLastTestedFilterDialog(isFromDialog = false)
            }

            StartPlayingButtonClicked -> {
                if (playerStateCreator.hasAnyCardAvailableForAutoplay()) {
                    navigator.navigateToPlayer {
                        val playerState: Player.State = playerStateCreator.create()
                        PlayerDiScope.create(playerState)
                    }
                } else {
                    sendCommand(ShowNoCardIsReadyForAutoplay)
                }
            }
        }
    }

    private fun showLastTestedFilterDialog(isFromDialog: Boolean) {
        navigator.showLastTestedFilterDialogFromCardFilterForAutoplay {
            val dateTimeSpan: DateTimeSpan? =
                if (isFromDialog) cardFilter.lastTestedFromTimeAgo
                else cardFilter.lastTestedToTimeAgo
            val dialogState = LastTestedFilterDialogState(
                isFromDialog = isFromDialog,
                isZeroTimeSelected = dateTimeSpan == null,
                timeAgo = dateTimeSpan?.let(DisplayedInterval.Companion::fromDateTimeSpan)
                    ?: DisplayedInterval.fromDateTimeSpan(7.days.toDateTimeSpan()),
                caller = LastTestedFilterDialogCaller.CardFilterForAutoplay
            )
            LastTestedFilterDiScope.create(dialogState)
        }
    }

    override fun saveState() {
        longTermStateSaver.saveStateByRegistry()
        playerCreatorStateProvider.save(playerStateCreator.state)
    }
}
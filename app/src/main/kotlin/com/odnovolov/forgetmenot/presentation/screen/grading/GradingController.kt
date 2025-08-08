package com.odnovolov.forgetmenot.presentation.screen.grading

import com.odnovolov.forgetmenot.domain.entity.GradeChangeOnCorrectAnswer
import com.odnovolov.forgetmenot.domain.entity.GradeChangeOnWrongAnswer
import com.odnovolov.forgetmenot.domain.interactor.decksettings.GradingSettings
import com.odnovolov.forgetmenot.presentation.common.LongTermStateSaver
import com.odnovolov.forgetmenot.presentation.common.Navigator
import com.odnovolov.forgetmenot.presentation.common.base.BaseController
import com.odnovolov.forgetmenot.presentation.screen.grading.GradingEvent.*
import com.odnovolov.forgetmenot.presentation.screen.grading.GradingScreenState.DialogPurpose.*
import com.odnovolov.forgetmenot.presentation.screen.helparticle.HelpArticle
import com.odnovolov.forgetmenot.presentation.screen.helparticle.HelpArticleDiScope
import com.odnovolov.forgetmenot.presentation.screen.helparticle.HelpArticleScreenState

class GradingController(
    private val gradingSettings: GradingSettings,
    private val screenState: GradingScreenState,
    private val navigator: Navigator,
    private val longTermStateSaver: LongTermStateSaver
) : BaseController<GradingEvent, Nothing>() {
    override fun handle(event: GradingEvent) {
        when (event) {
            HelpButtonClicked -> {
                navigator.navigateToHelpArticleFromGrading {
                    val screenState = HelpArticleScreenState(HelpArticle.GradesAndIntervals)
                    HelpArticleDiScope.create(screenState)
                }
            }

            CloseTipButtonClicked -> {
                screenState.tip?.state?.needToShow = false
                screenState.tip = null
            }

            FirstCorrectAnswerButtonClicked -> {
                screenState.dialogPurpose = ToChangeGradingOnFirstCorrectAnswer
                navigator.showChangeGradingDialog()
            }

            FirstWrongAnswerButtonClicked -> {
                screenState.dialogPurpose = ToChangeGradingOnFirstWrongAnswer
                navigator.showChangeGradingDialog()
            }

            YesAskAgainButtonClicked -> {
                gradingSettings.setAskAgain(true)
            }

            NoAskAgainButtonClicked -> {
                gradingSettings.setAskAgain(false)
            }

            RepeatedCorrectAnswerButtonClicked -> {
                screenState.dialogPurpose = ToChangeGradingOnRepeatedCorrectAnswer
                navigator.showChangeGradingDialog()
            }

            RepeatedWrongAnswerButtonClicked -> {
                screenState.dialogPurpose = ToChangeGradingOnRepeatedWrongAnswer
                navigator.showChangeGradingDialog()
            }

            is GradeChangeWasSelected -> {
                when (screenState.dialogPurpose) {
                    ToChangeGradingOnFirstCorrectAnswer -> {
                        val gradeChange = event.gradeChange as? GradeChangeOnCorrectAnswer ?: return
                        gradingSettings.setOnFirstCorrectAnswer(gradeChange)
                    }
                    ToChangeGradingOnFirstWrongAnswer -> {
                        val gradeChange = event.gradeChange as? GradeChangeOnWrongAnswer ?: return
                        gradingSettings.setOnFirstWrongAnswer(gradeChange)
                    }
                    ToChangeGradingOnRepeatedCorrectAnswer -> {
                        val gradeChange = event.gradeChange as? GradeChangeOnCorrectAnswer ?: return
                        gradingSettings.setOnRepeatedCorrectAnswer(gradeChange)
                    }
                    ToChangeGradingOnRepeatedWrongAnswer -> {
                        val gradeChange = event.gradeChange as? GradeChangeOnWrongAnswer ?: return
                        gradingSettings.setOnRepeatedWrongAnswer(gradeChange)
                    }
                    null -> return
                }
            }
        }
    }

    override fun saveState() {
        longTermStateSaver.saveStateByRegistry()
    }
}
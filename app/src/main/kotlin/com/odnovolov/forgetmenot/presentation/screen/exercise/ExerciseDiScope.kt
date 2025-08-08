package com.odnovolov.forgetmenot.presentation.screen.exercise

import com.odnovolov.forgetmenot.domain.interactor.exercise.Exercise
import com.odnovolov.forgetmenot.persistence.shortterm.ExerciseStateProvider
import com.odnovolov.forgetmenot.persistence.shortterm.ReadyToUseSerializableStateProvider
import com.odnovolov.forgetmenot.presentation.common.businessLogicThread
import com.odnovolov.forgetmenot.presentation.common.di.AppDiScope
import com.odnovolov.forgetmenot.presentation.common.di.DiScopeManager
import com.odnovolov.forgetmenot.presentation.screen.cardappearance.CardAppearance
import com.odnovolov.forgetmenot.presentation.screen.exercise.exercisecard.entry.EntryTestExerciseCardController
import com.odnovolov.forgetmenot.presentation.screen.exercise.exercisecard.manual.ManualTestExerciseCardController
import com.odnovolov.forgetmenot.presentation.screen.exercise.exercisecard.off.OffTestExerciseCardController
import com.odnovolov.forgetmenot.presentation.screen.exercise.exercisecard.quiz.QuizTestExerciseCardController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel

class ExerciseDiScope private constructor(
    initialExerciseState: Exercise.State? = null,
    initialExerciseScreenState: ExerciseScreenState? = null
) {
    val exerciseStateProvider = ExerciseStateProvider(
        AppDiScope.get().json,
        AppDiScope.get().database,
        AppDiScope.get().globalState,
        key = "ExerciseState"
    )

    val exerciseState: Exercise.State =
        initialExerciseState ?: exerciseStateProvider.load()

    private val exerciseScreenStateProvider = ReadyToUseSerializableStateProvider(
        ExerciseScreenState.serializer(),
        AppDiScope.get().json,
        AppDiScope.get().database,
        key = ExerciseScreenState::class.qualifiedName!!
    )

    private val screenState: ExerciseScreenState =
        initialExerciseScreenState ?: exerciseScreenStateProvider.load()

    val exercise = Exercise(
        exerciseState,
        AppDiScope.get().globalState,
        AppDiScope.get().speakerImpl,
        coroutineContext = Job() + businessLogicThread
    )

    private val cardAppearance: CardAppearance = AppDiScope.get().cardAppearance

    val controller = ExerciseController(
        exercise,
        AppDiScope.get().walkingModePreference,
        AppDiScope.get().exerciseSettings,
        AppDiScope.get().globalState,
        AppDiScope.get().navigator,
        screenState,
        AppDiScope.get().longTermStateSaver,
        exerciseStateProvider,
        exerciseScreenStateProvider
    )

    val viewModel = ExerciseViewModel(
        exerciseState,
        AppDiScope.get().speakerImpl,
        AppDiScope.get().walkingModePreference,
        AppDiScope.get().exerciseSettings,
        AppDiScope.get().globalState
    )

    private val offTestExerciseCardController = OffTestExerciseCardController(
        exercise,
        AppDiScope.get().longTermStateSaver,
        exerciseStateProvider
    )

    private val manualTestExerciseCardController = ManualTestExerciseCardController(
        exercise,
        AppDiScope.get().longTermStateSaver,
        exerciseStateProvider
    )

    private val quizTestExerciseCardController = QuizTestExerciseCardController(
        exercise,
        AppDiScope.get().longTermStateSaver,
        exerciseStateProvider
    )

    private val entryTestExerciseCardController = EntryTestExerciseCardController(
        exercise,
        AppDiScope.get().longTermStateSaver,
        exerciseStateProvider
    )

    fun getExerciseCardAdapter(
        coroutineScope: CoroutineScope
    ) = ExerciseCardAdapter(
        coroutineScope,
        offTestExerciseCardController,
        manualTestExerciseCardController,
        quizTestExerciseCardController,
        entryTestExerciseCardController,
        cardAppearance
    )

    companion object : DiScopeManager<ExerciseDiScope>() {
        fun create(initialExerciseState: Exercise.State) =
            ExerciseDiScope(initialExerciseState, ExerciseScreenState())

        override fun recreateDiScope() = ExerciseDiScope()

        override fun onCloseDiScope(diScope: ExerciseDiScope) {
            with(diScope) {
                AppDiScope.get().speakerImpl.stop()
                exercise.cancel()
                controller.dispose()
                offTestExerciseCardController.dispose()
                manualTestExerciseCardController.dispose()
                quizTestExerciseCardController.dispose()
                entryTestExerciseCardController.dispose()
            }
        }
    }
}
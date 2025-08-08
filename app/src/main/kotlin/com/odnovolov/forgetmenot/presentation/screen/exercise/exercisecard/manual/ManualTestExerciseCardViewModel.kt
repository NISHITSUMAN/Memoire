package com.odnovolov.forgetmenot.presentation.screen.exercise.exercisecard.manual

import com.odnovolov.forgetmenot.domain.architecturecomponents.share
import com.odnovolov.forgetmenot.domain.entity.Card
import com.odnovolov.forgetmenot.domain.interactor.exercise.ExerciseCard
import com.odnovolov.forgetmenot.domain.interactor.exercise.ManualTestExerciseCard
import com.odnovolov.forgetmenot.presentation.common.businessLogicThread
import com.odnovolov.forgetmenot.presentation.screen.exercise.exercisecard.CardLabel
import com.odnovolov.forgetmenot.presentation.screen.exercise.exercisecard.manual.CardContent.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

class ManualTestExerciseCardViewModel(
    initialExerciseCard: ManualTestExerciseCard
) {
    private val exerciseCardFlow = MutableStateFlow(initialExerciseCard)

    fun setExerciseCard(exerciseCard: ManualTestExerciseCard) {
        exerciseCardFlow.value = exerciseCard
    }

    val cardContent: Flow<CardContent> = exerciseCardFlow.flatMapLatest { exerciseCard ->
        combine(
            exerciseCard.base.card.flowOf(Card::question),
            exerciseCard.base.card.flowOf(Card::answer),
            exerciseCard.base.flowOf(ExerciseCard.Base::isInverted),
            exerciseCard.base.flowOf(ExerciseCard.Base::hint),
            exerciseCard.base.flowOf(ExerciseCard.Base::isAnswerCorrect)
        ) { question: String,
            answer: String,
            isInverted: Boolean,
            hint: String?,
            isAnswerCorrect: Boolean?
            ->
            val realQuestion = if (isInverted) answer else question
            val realAnswer = if (isInverted) question else answer
            when {
                isAnswerCorrect != null -> AnsweredCard(realQuestion, realAnswer)
                hint != null -> UnansweredCardWithHint(realQuestion, hint)
                else -> UnansweredCard(realQuestion)
            }
        }
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    val isQuestionDisplayed: Flow<Boolean> =
        exerciseCardFlow.flatMapLatest { exerciseCard ->
            exerciseCard.base.flowOf(ExerciseCard.Base::isQuestionDisplayed)
        }
            .distinctUntilChanged()
            .flowOn(Dispatchers.Default)

    val isAnswerCorrect: Flow<Boolean?> = exerciseCardFlow.flatMapLatest { exerciseCard ->
        exerciseCard.base.flowOf(ExerciseCard.Base::isAnswerCorrect)
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    val isExpired: Flow<Boolean> = exerciseCardFlow.flatMapLatest { exerciseCard ->
        exerciseCard.base.flowOf(ExerciseCard.Base::isExpired)
    }
        .distinctUntilChanged()
        .share()
        .flowOn(Dispatchers.Default)

    val isLearned: Flow<Boolean> = exerciseCardFlow.flatMapLatest { exerciseCard ->
        exerciseCard.base.card.flowOf(Card::isLearned)
    }
        .distinctUntilChanged()
        .share()
        .flowOn(Dispatchers.Default)

    val cardLabel: Flow<CardLabel?> = combine(isLearned, isExpired) { isLearned: Boolean,
                                                                      isExpired: Boolean ->
        when {
            isLearned -> CardLabel.Learned
            isExpired -> CardLabel.Expired
            else -> null
        }
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
}
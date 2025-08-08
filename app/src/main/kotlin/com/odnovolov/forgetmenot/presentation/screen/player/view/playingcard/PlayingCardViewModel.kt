package com.odnovolov.forgetmenot.presentation.screen.player.view.playingcard

import com.odnovolov.forgetmenot.domain.entity.Card
import com.odnovolov.forgetmenot.domain.interactor.autoplay.PlayingCard
import com.odnovolov.forgetmenot.presentation.screen.player.view.playingcard.CardContent.AnsweredCard
import com.odnovolov.forgetmenot.presentation.screen.player.view.playingcard.CardContent.UnansweredCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*

class PlayingCardViewModel(
    initialPlayingCard: PlayingCard
) {
    private val playingCardFlow = MutableStateFlow(initialPlayingCard)

    fun setPlayingCard(playingCard: PlayingCard) {
        playingCardFlow.value = playingCard
    }

    val cardContent: Flow<CardContent> = playingCardFlow.flatMapLatest { playingCard ->
        combine(
            playingCard.card.flowOf(Card::question),
            playingCard.card.flowOf(Card::answer),
            playingCard.flowOf(PlayingCard::isInverted),
            playingCard.flowOf(PlayingCard::isAnswerDisplayed)
        ) { question: String,
            answer: String,
            isInverted: Boolean,
            isAnswerDisplayed: Boolean
            ->
            val realQuestion = if (isInverted) answer else question
            val realAnswer = if (isInverted) question else answer
            when {
                isAnswerDisplayed -> AnsweredCard(realQuestion, realAnswer)
                else -> UnansweredCard(realQuestion)
            }
        }
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    val isQuestionDisplayed: Flow<Boolean> = playingCardFlow.flatMapLatest { playingCard ->
        playingCard.flowOf(PlayingCard::isQuestionDisplayed)
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    val isLearned: Flow<Boolean> = playingCardFlow.flatMapLatest { playingCard ->
        playingCard.card.flowOf(Card::isLearned)
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
}
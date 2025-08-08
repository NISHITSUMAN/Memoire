package com.odnovolov.forgetmenot.presentation.screen.exercise.exercisecard.manual

sealed class CardContent {
    data class UnansweredCard(
        val question: String
    ) : CardContent()

    data class UnansweredCardWithHint(
        val question: String,
        val hint: String
    ) : CardContent()

    data class AnsweredCard(
        val question: String,
        val answer: String
    ) : CardContent()
}
package com.odnovolov.forgetmenot.presentation.screen.pronunciation

import java.util.*

data class DisplayedLanguage(
    val language: Locale?,
    val isSelected: Boolean,
    val isFavorite: Boolean?
)
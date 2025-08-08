package com.odnovolov.forgetmenot.presentation.screen.pronunciation

import com.odnovolov.forgetmenot.domain.interactor.decksettings.PronunciationSettings
import com.odnovolov.forgetmenot.presentation.common.LongTermStateSaver
import com.odnovolov.forgetmenot.presentation.common.Navigator
import com.odnovolov.forgetmenot.presentation.common.ShortTermStateProvider
import com.odnovolov.forgetmenot.presentation.common.base.BaseController
import com.odnovolov.forgetmenot.presentation.screen.helparticle.HelpArticle
import com.odnovolov.forgetmenot.presentation.screen.helparticle.HelpArticleDiScope
import com.odnovolov.forgetmenot.presentation.screen.helparticle.HelpArticleScreenState
import com.odnovolov.forgetmenot.presentation.screen.pronunciation.PronunciationEvent.*

class PronunciationController(
    private val pronunciationSettings: PronunciationSettings,
    private val screenState: PronunciationScreenState,
    private val pronunciationPreference: PronunciationPreference,
    private val navigator: Navigator,
    private val longTermStateSaver: LongTermStateSaver,
    private val screenStateProvider: ShortTermStateProvider<PronunciationScreenState>
) : BaseController<PronunciationEvent, Nothing>() {
    override fun handle(event: PronunciationEvent) = when (event) {
        HelpButtonClicked -> {
            navigator.navigateToHelpArticleFromPronunciation {
                val screenState = HelpArticleScreenState(HelpArticle.Pronunciation)
                HelpArticleDiScope.create(screenState)
            }
        }

        CloseTipButtonClicked -> {
            screenState.tip?.state?.needToShow = false
            screenState.tip = null
        }

        is QuestionLanguageWasSelected -> {
            pronunciationSettings.setQuestionLanguage(event.language)
        }

        QuestionAutoSpeakSwitchToggled -> {
            pronunciationSettings.toggleQuestionAutoSpeaking()
        }

        is AnswerLanguageWasSelected -> {
            pronunciationSettings.setAnswerLanguage(event.language)
        }

        AnswerAutoSpeakSwitchToggled -> {
            pronunciationSettings.toggleAnswerAutoSpeaking()
        }

        SpeakTextInBracketsSwitchToggled -> {
            pronunciationSettings.toggleSpeakTextInBrackets()
        }

        is LanguageWasMarkedAsFavorite -> {
            pronunciationPreference.favoriteLanguages += event.language
        }

        is LanguageWasUnmarkedAsFavorite -> {
            pronunciationPreference.favoriteLanguages -= event.language
        }
    }

    override fun saveState() {
        longTermStateSaver.saveStateByRegistry()
        screenStateProvider.save(screenState)
    }
}
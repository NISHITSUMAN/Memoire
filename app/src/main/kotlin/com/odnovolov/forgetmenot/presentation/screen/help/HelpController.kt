package com.odnovolov.forgetmenot.presentation.screen.help

import com.odnovolov.forgetmenot.presentation.common.Navigator
import com.odnovolov.forgetmenot.presentation.common.base.BaseController
import com.odnovolov.forgetmenot.presentation.screen.help.HelpEvent.ArticleItemClicked
import com.odnovolov.forgetmenot.presentation.screen.helparticle.HelpArticleDiScope
import com.odnovolov.forgetmenot.presentation.screen.helparticle.HelpArticleScreenState

class HelpController(
    private val navigator: Navigator
) : BaseController<HelpEvent, Nothing>() {
    override val autoSave = false

    override fun handle(event: HelpEvent) {
        when (event) {
            is ArticleItemClicked -> {
                navigator.navigateToHelpArticleFromNavHost {
                    val screenState = HelpArticleScreenState(event.helpArticle)
                    HelpArticleDiScope.create(screenState)
                }
            }
        }
    }

    override fun saveState() {}
}
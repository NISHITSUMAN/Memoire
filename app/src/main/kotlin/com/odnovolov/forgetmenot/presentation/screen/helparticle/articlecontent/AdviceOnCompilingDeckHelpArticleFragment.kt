package com.odnovolov.forgetmenot.presentation.screen.helparticle.articlecontent

import android.text.method.LinkMovementMethod
import com.odnovolov.forgetmenot.R
import com.odnovolov.forgetmenot.presentation.screen.helparticle.HelpArticle
import kotlinx.android.synthetic.main.article.*

class AdviceOnCompilingDeckHelpArticleFragment : BaseHelpArticleFragmentForSimpleUi() {
    override val layoutRes: Int get() = R.layout.article
    override val helpArticle: HelpArticle get() = HelpArticle.AdviceOnCompilingDeck

    override fun setupView() {
        super.setupView()
        articleContentTextView.run {
            setText(R.string.article_advice_on_compiling_deck)
            movementMethod = LinkMovementMethod.getInstance()
        }
    }
}
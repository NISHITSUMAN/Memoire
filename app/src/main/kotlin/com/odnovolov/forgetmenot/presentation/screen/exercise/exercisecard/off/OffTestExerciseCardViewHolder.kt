package com.odnovolov.forgetmenot.presentation.screen.exercise.exercisecard.off

import android.animation.AnimatorInflater
import android.animation.LayoutTransition
import android.util.Size
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import com.odnovolov.forgetmenot.R
import com.odnovolov.forgetmenot.domain.interactor.exercise.OffTestExerciseCard
import com.odnovolov.forgetmenot.presentation.common.*
import com.odnovolov.forgetmenot.presentation.common.base.BaseController
import com.odnovolov.forgetmenot.presentation.screen.cardappearance.CardAppearance
import com.odnovolov.forgetmenot.presentation.screen.cardappearance.setCardTextColorStateList
import com.odnovolov.forgetmenot.presentation.screen.exercise.exercisecard.AsyncCardFrame
import com.odnovolov.forgetmenot.presentation.screen.exercise.exercisecard.CardLabel
import com.odnovolov.forgetmenot.presentation.screen.exercise.exercisecard.CardSpaceAllocator
import com.odnovolov.forgetmenot.presentation.screen.exercise.exercisecard.ExerciseCardViewHolder
import com.odnovolov.forgetmenot.presentation.screen.exercise.exercisecard.manual.CardContent
import com.odnovolov.forgetmenot.presentation.screen.exercise.exercisecard.manual.CardContent.*
import com.odnovolov.forgetmenot.presentation.screen.exercise.exercisecard.off.OffTestExerciseCardEvent.*
import kotlinx.android.synthetic.main.item_exercise_card_off_test.view.*
import kotlinx.android.synthetic.main.popup_card_label_tip.view.*
import kotlinx.coroutines.CoroutineScope

class OffTestExerciseCardViewHolder(
    private val asyncItemView: AsyncCardFrame,
    private val coroutineScope: CoroutineScope,
    private val controller: BaseController<OffTestExerciseCardEvent, Nothing>,
    private val cardAppearance: CardAppearance
) : ExerciseCardViewHolder<OffTestExerciseCard>(
    asyncItemView
) {
    private val cardLabelTipPopup: PopupWindow by lazy {
        val content = View.inflate(asyncItemView.context, R.layout.popup_card_label_tip, null)
        PopupWindow(content).apply {
            setBackgroundDrawable(null)
            isOutsideTouchable = true
            isFocusable = true
            animationStyle = R.style.AnimationCardLabel
        }
    }

    private val qTextView by lazy {
        TextView(itemView.context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            setPadding(16.dp)
            textSize = cardAppearance.questionTextSize.toFloat()
        }
    }

    private val aTextView by lazy {
        TextView(itemView.context).apply {
            layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            setPadding(16.dp)
            textSize = cardAppearance.answerTextSize.toFloat()
        }
    }

    private var cardContent: CardContent? = null
        set(value) {
            field = value
            updateCardContent()
        }

    private var cardSize: Size? = null
        set(value) {
            itemView.post {
                if (field != value) {
                    field = value
                    updateCardContent()
                }
            }
        }

    private var needToResetRippleOnScrolling = true

    init {
        asyncItemView.invokeWhenReady {
            cardView.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                cardSize = Size(cardView.width, cardView.height)
            }
            setupView()
        }
    }

    private fun setupView() {
        with(asyncItemView) {
            cardLinearLayout.layoutTransition.run {
                enableTransitionType(LayoutTransition.CHANGING)
                disableTransitionType(LayoutTransition.APPEARING)
                disableTransitionType(LayoutTransition.DISAPPEARING)
                disableTransitionType(LayoutTransition.CHANGE_APPEARING)
                disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING)
            }
            showQuestionButton.setOnClickListener {
                controller.dispatch(ShowQuestionButtonClicked)
            }
            questionTextView.gravity = cardAppearance.questionTextAlignment.gravity
            questionTextView.textSize = cardAppearance.questionTextSize.toFloat()
            questionTextView.setCardTextColorStateList(cardAppearance)
            questionTextView.observeSelectedText { selection: String ->
                controller.dispatch(QuestionTextSelectionChanged(selection))
            }
            showAnswerButton.setOnClickListener {
                controller.dispatch(ShowAnswerButtonClicked)
            }
            hintTextView.observeSelectedRange { startIndex: Int, endIndex: Int ->
                controller.dispatch(HintSelectionChanged(startIndex, endIndex))
            }
            hintTextView.gravity = cardAppearance.answerTextAlignment.gravity
            hintTextView.textSize = cardAppearance.answerTextSize.toFloat()
            hintTextView.setCardTextColorStateList(cardAppearance)
            answerTextView.observeSelectedText { selection: String ->
                controller.dispatch(AnswerTextSelectionChanged(selection))
            }
            answerTextView.gravity = cardAppearance.answerTextAlignment.gravity
            answerTextView.textSize = cardAppearance.answerTextSize.toFloat()
            answerTextView.setCardTextColorStateList(cardAppearance)
            cardLabelTextView.stateListAnimator =
                AnimatorInflater.loadStateListAnimator(context, R.animator.card_label)
            addScrollListener {
                if (x == 0f) {
                    needToResetRippleOnScrolling = true
                } else {
                    if (needToResetRippleOnScrolling) {
                        needToResetRippleOnScrolling = false
                        showQuestionButton.jumpDrawablesToCurrentState()
                        showAnswerButton.jumpDrawablesToCurrentState()
                    }
                }
            }
        }
    }

    private var viewModel: OffTestExerciseCardViewModel? = null

    override fun bind(exerciseCard: OffTestExerciseCard) {
        asyncItemView.invokeWhenReady {
            if (viewModel == null) {
                viewModel = OffTestExerciseCardViewModel(exerciseCard)
                observeViewModel()
            } else {
                questionScrollView.scrollTo(0, 0)
                hintScrollView.scrollTo(0, 0)
                answerScrollView.scrollTo(0, 0)
                viewModel!!.setExerciseCard(exerciseCard)
            }
        }
    }

    private fun observeViewModel() {
        with(viewModel!!) {
            with(itemView) {
                cardContent.observe(coroutineScope) { cardContent: CardContent ->
                    this@OffTestExerciseCardViewHolder.cardContent = cardContent
                }
                isQuestionDisplayed.observe(coroutineScope) { isQuestionDisplayed: Boolean ->
                    showQuestionButton.isVisible = !isQuestionDisplayed
                    questionScrollView.isInvisible = !isQuestionDisplayed
                }
                isExpired.observe(coroutineScope) { isExpired: Boolean ->
                    val cardBackgroundColorRes: Int =
                        if (isExpired)
                            R.color.card_expired else
                            R.color.card
                    val cardBackgroundColor: Int =
                        ContextCompat.getColor(context, cardBackgroundColorRes)
                    cardView.setCardBackgroundColor(cardBackgroundColor)
                }
                isLearned.observe(coroutineScope) { isLearned: Boolean ->
                    val isEnabled = !isLearned
                    showQuestionButton.isEnabled = isEnabled
                    questionTextView.isEnabled = isEnabled
                    showAnswerButton.isEnabled = isEnabled
                    hintTextView.isEnabled = isEnabled
                    answerTextView.isEnabled = isEnabled
                }
                cardLabel.observe(coroutineScope) { cardLabel: CardLabel? ->
                    when (cardLabel) {
                        CardLabel.Learned -> {
                            cardLabelTextView.setText(R.string.card_label_learned)
                            cardLabelTextView.backgroundTintList =
                                ContextCompat.getColorStateList(context, R.color.card_label_learned)
                            cardLabelTextView.setOnClickListener {
                                showCardLabelTipPopup(cardLabel)
                            }
                            cardLabelTextView.isEnabled = true
                        }
                        CardLabel.Expired -> {
                            cardLabelTextView.setText(R.string.card_label_expired)
                            cardLabelTextView.backgroundTintList =
                                ContextCompat.getColorStateList(context, R.color.card_label_expired)
                            cardLabelTextView.setOnClickListener {
                                showCardLabelTipPopup(cardLabel)
                            }
                            cardLabelTextView.isEnabled = true
                        }
                        null -> {
                            cardLabelTextView.isEnabled = false
                        }
                    }
                }
            }
        }
    }

    private fun updateCardContent() {
        val cardContent = cardContent ?: return
        val cardSize = cardSize ?: return
        when (cardContent) {
            is UnansweredCard -> {
                val availableCardHeight = cardSize.height - 1.dp
                val desiredQuestionFrameHeight = measureHeight(qTextView, cardContent.question)
                val desiredAnswerFrameHeight = 48.dp
                CardSpaceAllocator.allocate(
                    availableCardHeight,
                    itemView.questionFrame,
                    desiredQuestionFrameHeight,
                    itemView.answerFrame,
                    desiredAnswerFrameHeight
                )
                itemView.hintScrollView.isVisible = false
                itemView.hintDivider.isVisible = false
                itemView.showAnswerButton.isVisible = true
                itemView.answerScrollView.isVisible = false
                itemView.questionTextView.text = cardContent.question
                itemView.questionTextView.fixTextSelection()
            }
            is UnansweredCardWithHint -> {
                itemView.answerFrame.updateLayoutParams<LinearLayout.LayoutParams> {
                    height = 48.dp
                    weight = 0f
                }
                val availableCardHeight = cardSize.height - 50.dp
                val desiredQuestionFrameHeight = measureHeight(qTextView, cardContent.question)
                val desiredHintFrameHeight = measureHeight(aTextView, cardContent.hint)
                CardSpaceAllocator.allocate(
                    availableCardHeight,
                    itemView.questionFrame,
                    desiredQuestionFrameHeight,
                    itemView.hintScrollView,
                    desiredHintFrameHeight
                )
                itemView.hintScrollView.isVisible = true
                itemView.hintDivider.isVisible = true
                itemView.showAnswerButton.isVisible = true
                itemView.answerScrollView.isVisible = false
                itemView.questionTextView.text = cardContent.question
                itemView.questionTextView.fixTextSelection()
                itemView.hintTextView.text = cardContent.hint
                itemView.hintTextView.fixTextSelection()
            }
            is AnsweredCard -> {
                val availableCardHeight = cardSize.height - 1.dp
                val desiredQuestionFrameHeight = measureHeight(qTextView, cardContent.question)
                val desiredAnswerFrameHeight = measureHeight(aTextView, cardContent.answer)
                CardSpaceAllocator.allocate(
                    availableCardHeight,
                    itemView.questionFrame,
                    desiredQuestionFrameHeight,
                    itemView.answerFrame,
                    desiredAnswerFrameHeight
                )
                itemView.hintScrollView.isVisible = false
                itemView.hintDivider.isVisible = false
                itemView.showAnswerButton.isVisible = false
                itemView.answerScrollView.isVisible = true
                itemView.questionTextView.text = cardContent.question
                itemView.questionTextView.fixTextSelection()
                itemView.answerTextView.text = cardContent.answer
                itemView.answerTextView.fixTextSelection()
            }
        }
    }

    private fun measureHeight(textView: TextView, question: String): Int {
        textView.text = question
        textView.measure(
            MeasureSpec.makeMeasureSpec(cardSize!!.width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        return textView.measuredHeight
    }

    private fun showCardLabelTipPopup(cardLabel: CardLabel) {
        with(cardLabelTipPopup) {
            contentView.cardLabelExplanationTextView.setText(
                when (cardLabel) {
                    CardLabel.Learned -> R.string.explanation_card_label_learned
                    CardLabel.Expired -> R.string.explanation_card_label_expired
                }
            )
            contentView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
            width = contentView.measuredWidth
            height = contentView.measuredHeight
            val xOff: Int = itemView.cardLabelTextView.width / 2 - width / 2
            val yOff: Int = 8.dp
            showAsDropDown(itemView.cardLabelTextView, xOff, yOff)
        }
    }
}
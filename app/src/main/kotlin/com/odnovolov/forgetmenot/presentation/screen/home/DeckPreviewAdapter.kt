package com.odnovolov.forgetmenot.presentation.screen.home

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.view.View.MeasureSpec.makeMeasureSpec
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
import com.odnovolov.forgetmenot.R
import com.odnovolov.forgetmenot.presentation.common.*
import com.odnovolov.forgetmenot.presentation.common.customview.AsyncFrameLayout
import com.odnovolov.forgetmenot.presentation.screen.home.DeckListItem.DeckPreview
import kotlinx.android.synthetic.main.item_deck_preview.view.*

class DeckPreviewAdapter(
    private val createHeader: (parent: ViewGroup) -> View,
    private val onDeckButtonClicked: (deckId: Long) -> Unit,
    private val onDeckButtonLongClicked: ((deckId: Long) -> Unit)? = null,
    private val onDeckOptionButtonClicked: ((deckId: Long) -> Unit)? = null,
    private val onDeckSelectorClicked: ((deckId: Long) -> Unit)? = null
) : ListAdapter<DeckListItem, SimpleRecyclerViewHolder>(DiffCallback()) {
    init {
        stateRestorationPolicy = PREVENT_WHEN_EMPTY
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
        private const val TYPE_FOOTER = 2
    }

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            DeckListItem.Header -> TYPE_HEADER
            DeckListItem.Footer -> TYPE_FOOTER
            else -> TYPE_ITEM
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimpleRecyclerViewHolder {
        prepareMeasuring(parent)

        val layoutInflater = LayoutInflater.from(parent.context)
        val view = when (viewType) {
            TYPE_HEADER -> {
                createHeader(parent)
            }
            TYPE_FOOTER -> {
                layoutInflater.inflate(R.layout.item_deck_preview_footer, parent, false)
            }
            else -> {
                val layoutParams = LayoutParams(MATCH_PARENT, 0)
                AsyncFrameLayout(layoutParams, parent.context).apply {
                    inflateAsync(R.layout.item_deck_preview)
                    invokeWhenInflated {
                        // strangely enough, text size in xml sometimes differs from text size that
                        // set programmatically. It may cause different height of real item and
                        // premeasured height of AsyncFrameLayout
                        deckNameTextView.setTextSizeFromRes(R.dimen.text_size_home_screen_deck_name)
                    }
                }
            }
        }
        return SimpleRecyclerViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: SimpleRecyclerViewHolder, position: Int) {
        val deckListItem = getItem(position)
        when (deckListItem) {
            DeckListItem.Header, DeckListItem.Footer -> return
        }
        val asyncFrameLayout = viewHolder.itemView as AsyncFrameLayout
        val deckPreview = deckListItem as DeckPreview
        measureAsyncFrameLayoutHeight(asyncFrameLayout, deckPreview.deckName)
        asyncFrameLayout.invokeWhenInflated {
            deckButton.setOnClickListener {
                onDeckButtonClicked(deckPreview.deckId)
            }
            onDeckButtonLongClicked?.let { onDeckButtonLongClicked ->
                deckButton.setOnLongClickListener {
                    onDeckButtonLongClicked(deckPreview.deckId)
                    true
                }
            }
            deckNameTextView.text =
                if (deckPreview.searchMatchingRanges != null)
                    deckPreview.deckName.highlight(deckPreview.searchMatchingRanges, context) else
                    deckPreview.deckName
            onDeckOptionButtonClicked?.let { onDeckOptionButtonClicked ->
                deckOptionButton.setOnClickListener {
                    onDeckOptionButtonClicked(deckPreview.deckId)
                }
            }
            onDeckSelectorClicked?.let { onDeckSelectorClicked ->
                deckSelector.setOnClickListener {
                    onDeckSelectorClicked(deckPreview.deckId)
                }
            }
            avgLapsValueTextView.text = deckPreview.averageLaps
            learnedValueTextView.text = "${deckPreview.learnedCount}/${deckPreview.totalCount}"
            taskValueTextView.text = deckPreview.numberOfCardsReadyForExercise?.toString() ?: "-"
            taskValueTextView.setTextColor(
                getTaskColor(deckPreview.numberOfCardsReadyForExercise, context)
            )
            val isDeckNew = deckPreview.lastTestedAt == null
            lastTestedValueTextView.isVisible = !isDeckNew
            newDeckLabelTextView.isVisible = isDeckNew
            if (!isDeckNew) {
                lastTestedValueTextView.text = deckPreview.lastTestedAt
            }
            val deckListIcon: Drawable = DeckListDrawableGenerator.generateIcon(
                strokeColors = deckPreview.deckListColors,
                backgroundColor = ContextCompat.getColor(context, R.color.surface)
            )
            deckNameTextView.setCompoundDrawablesRelative(deckListIcon, null, null, null)
            updateDeckItemSelectionState(itemView = this, deckPreview.deckId)
            pinIcon.isVisible = deckPreview.isPinned
        }
    }

    private var colorNotHasTask: Int? = null
    private var colorHasTask: Int? = null

    private fun getTaskColor(numberOfCardsReadyForExercise: Int?, context: Context): Int {
        return if (numberOfCardsReadyForExercise == null || numberOfCardsReadyForExercise == 0) {
            if (colorNotHasTask == null) {
                colorNotHasTask = ContextCompat.getColor(context, R.color.text_high_emphasis)
            }
            colorNotHasTask!!
        } else {
            if (colorHasTask == null) {
                colorHasTask = ContextCompat.getColor(context, R.color.task)
            }
            colorHasTask!!
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<DeckListItem>() {
        override fun areItemsTheSame(oldItem: DeckListItem, newItem: DeckListItem): Boolean {
            return when {
                oldItem === newItem -> true
                oldItem is DeckPreview && newItem is DeckPreview -> {
                    oldItem.deckId == newItem.deckId
                }
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: DeckListItem, newItem: DeckListItem): Boolean {
            return oldItem == newItem
        }
    }

    // Deck selection

    var deckSelection: DeckSelection? = null
        set(value) {
            field = value
            itemViewDeckIdMap.forEach { (itemView: View, deckId: Long) ->
                updateDeckItemSelectionState(itemView, deckId)
            }
        }

    private var itemViewDeckIdMap = HashMap<View, Long>()

    private fun updateDeckItemSelectionState(itemView: View, deckId: Long) {
        itemViewDeckIdMap[itemView] = deckId

        val isItemSelected: Boolean? = deckSelection?.run {
            deckId in selectedDeckIds
        }
        itemView.isSelected = isItemSelected == true
        itemView.deckOptionButton.isVisible =
            isItemSelected == null && onDeckOptionButtonClicked != null
        itemView.deckSelector.isVisible = isItemSelected != null && onDeckSelectorClicked != null
    }

    // end Deck selection

    // Measuring

    private var parentWidth = -1
    private var textViewForMeasure: TextView? = null

    private fun prepareMeasuring(parent: ViewGroup) {
        if (parentWidth != parent.width) {
            parentWidth = parent.width
            textViewForMeasure = TextView(parent.context).apply {
                layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                setTextSizeFromRes(R.dimen.text_size_home_screen_deck_name)
                setFont(R.font.nunito_bold)
            }
        }
    }

    private fun measureAsyncFrameLayoutHeight(
        asyncFrameLayout: AsyncFrameLayout,
        deckName: String
    ) {
        if (asyncFrameLayout.isInflated) {
            if (asyncFrameLayout.layoutParams.height != WRAP_CONTENT) {
                asyncFrameLayout.updateLayoutParams {
                    height = WRAP_CONTENT
                }
            }
            return
        }
        val widthForDeckNameTextView: Int = parentWidth - 118.dp

        val textViewForMeasure = textViewForMeasure!!
        textViewForMeasure.text = deckName
        textViewForMeasure.measure(
            makeMeasureSpec(widthForDeckNameTextView, MeasureSpec.EXACTLY),
            makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        val asyncFrameLayoutHeight = 57.dp + 29.sp + textViewForMeasure.measuredHeight
        asyncFrameLayout.updateLayoutParams {
            height = asyncFrameLayoutHeight
        }
    }

    // end Measuring
}
package com.odnovolov.forgetmenot.presentation.screen.home.deckoptions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.odnovolov.forgetmenot.R
import com.odnovolov.forgetmenot.presentation.common.base.BaseBottomSheetDialogFragment
import com.odnovolov.forgetmenot.presentation.screen.home.DeckListDrawableGenerator
import com.odnovolov.forgetmenot.presentation.screen.home.HomeController
import com.odnovolov.forgetmenot.presentation.screen.home.HomeDiScope
import com.odnovolov.forgetmenot.presentation.screen.home.HomeEvent.*
import kotlinx.android.synthetic.main.bottom_sheet_deck_options.*
import kotlinx.coroutines.launch

class DeckOptionsBottomSheet : BaseBottomSheetDialogFragment() {
    init {
        HomeDiScope.reopenIfClosed()
    }

    private var controller: HomeController? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_deck_options, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        viewCoroutineScope!!.launch {
            val diScope = HomeDiScope.getAsync() ?: return@launch
            controller = diScope.controller
            observeViewModel(diScope.deckOptionsViewModel)
        }
    }

    private fun setupView() {
        startExerciseDeckOptionItem.setOnClickListener {
            controller?.dispatch(StartExerciseDeckOptionWasSelected)
            dismiss()
        }
        autoplayDeckOptionItem.setOnClickListener {
            controller?.dispatch(AutoplayDeckOptionWasSelected)
            dismiss()
        }
        renameDeckOptionItem.setOnClickListener {
            controller?.dispatch(RenameDeckOptionWasSelected)
            dismiss()
        }
        setupDeckOptionItem.setOnClickListener {
            controller?.dispatch(SetupDeckOptionWasSelected)
            dismiss()
        }
        editCardsDeckOptionItem.setOnClickListener {
            controller?.dispatch(EditCardsDeckOptionWasSelected)
            dismiss()
        }
        addToDeckListDeckOptionItem.setOnClickListener {
            controller?.dispatch(AddToDeckListDeckOptionWasSelected)
            dismiss()
        }
        removeFromDeckListDeckOptionItem.setOnClickListener {
            controller?.dispatch(RemoveFromDeckListDeckOptionWasSelected)
            dismiss()
        }
        exportDeckOptionItem.setOnClickListener {
            controller?.dispatch(ExportDeckOptionWasSelected)
            dismiss()
        }
        mergeIntoDeckOptionItem.setOnClickListener {
            controller?.dispatch(MergeIntoDeckOptionWasSelected)
            dismiss()
        }
        removeDeckOptionItem.setOnClickListener {
            controller?.dispatch(RemoveDeckOptionWasSelected)
            dismiss()
        }
    }

    private fun observeViewModel(viewModel: DeckOptionsViewModel) {
        with(viewModel) {
            deckListIndicatorColors.observe { deckListColors: List<Int> ->
                deckListIndicator.background = DeckListDrawableGenerator.generateIcon(
                    strokeColors = deckListColors,
                    backgroundColor = ContextCompat.getColor(requireContext(), R.color.dialog)
                )
            }
            deckName.observe { deckName: String? ->
                if (deckName != null) {
                    deckNameTextView.text = deckName
                }
            }
            isDeckPinned.observe { isPinned: Boolean ->
                pinDeckOptionItem.setText(
                    if (isPinned)
                        R.string.deck_option_unpin else
                        R.string.deck_option_pin
                )
                pinDeckOptionItem.setOnClickListener {
                    controller?.dispatch(
                        if (isPinned)
                            UnpinDeckOptionWasSelected else
                            PinDeckOptionWasSelected
                    )
                    dismiss()
                }
                val drawableResStart: Int =
                    if (isPinned)
                        R.drawable.ic_outline_push_pin_20 else
                        R.drawable.ic_round_push_pin_20
                pinDeckOptionItem.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    drawableResStart, 0, 0, 0
                )
            }
            namesOfDeckListsToWhichDeckBelongs.observe { namesOfDeckLists: List<String> ->
                removeFromDeckListDeckOptionItem.isVisible = namesOfDeckLists.isNotEmpty()
                if (namesOfDeckLists.size == 1) {
                    removeFromDeckListDeckOptionItem.text = getString(
                        R.string.deck_option_remove_from_deck_list_with_arg,
                        namesOfDeckLists[0]
                    )
                }
            }
        }
    }
}
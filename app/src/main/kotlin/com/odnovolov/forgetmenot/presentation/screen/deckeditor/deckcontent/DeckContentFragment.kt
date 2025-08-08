package com.odnovolov.forgetmenot.presentation.screen.deckeditor.deckcontent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentStateRestorer
import androidx.recyclerview.widget.RecyclerView
import com.odnovolov.forgetmenot.R
import com.odnovolov.forgetmenot.presentation.common.*
import com.odnovolov.forgetmenot.presentation.common.base.BaseFragment
import kotlinx.android.synthetic.main.fragment_deck_content.*
import kotlinx.coroutines.launch

class DeckContentFragment : BaseFragment() {
    init {
        DeckContentDiScope.reopenIfClosed()
    }

    private var controller: DeckContentController? = null
    private lateinit var viewModel: DeckContentViewModel
    private var isInflated = false
    private val fragmentStateRestorer = FragmentStateRestorer(this)
    var scrollListener: RecyclerView.OnScrollListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentStateRestorer.interceptSavedState()
        return inflater.inflateAsync(R.layout.fragment_deck_content, ::onViewInflated)
    }

    private fun onViewInflated() {
        if (viewCoroutineScope != null) {
            isInflated = true
            setupIfReady()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewCoroutineScope!!.launch {
            val diScope = DeckContentDiScope.getAsync() ?: return@launch
            controller = diScope.controller
            viewModel = diScope.viewModel
            setupIfReady()
        }
    }

    private fun setupIfReady() {
        if (viewCoroutineScope == null || controller == null || !isInflated) return
        fragmentStateRestorer.restoreState()
        val adapter = CardOverviewAdapter(controller!!)
        cardsRecycler.adapter = adapter
        viewModel.cards.observe { cards: List<ItemInDeckContentList> ->
            adapter.submitList(cards)
            emptyTextView.isVisible = cards.isEmpty()
        }
        scrollListener?.let(cardsRecycler::addOnScrollListener)
    }

    override fun onDestroyView() {
        if (isInflated) scrollListener?.let(cardsRecycler::removeOnScrollListener)
        super.onDestroyView()
        isInflated = false
    }
}
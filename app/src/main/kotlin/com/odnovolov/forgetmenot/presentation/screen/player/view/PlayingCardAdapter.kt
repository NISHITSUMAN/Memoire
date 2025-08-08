package com.odnovolov.forgetmenot.presentation.screen.player.view

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.odnovolov.forgetmenot.R
import com.odnovolov.forgetmenot.domain.interactor.autoplay.PlayingCard
import com.odnovolov.forgetmenot.presentation.screen.cardappearance.CardAppearance
import com.odnovolov.forgetmenot.presentation.screen.exercise.exercisecard.AsyncCardFrame
import com.odnovolov.forgetmenot.presentation.screen.player.view.playingcard.PlayingCardController
import com.odnovolov.forgetmenot.presentation.screen.player.view.playingcard.PlayingCardViewHolder
import kotlinx.coroutines.CoroutineScope

class PlayingCardAdapter(
    private val coroutineScope: CoroutineScope,
    private val playingCardController: PlayingCardController,
    private val cardAppearance: CardAppearance
) : RecyclerView.Adapter<PlayingCardViewHolder>() {
    var items: List<PlayingCard> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayingCardViewHolder {
        val itemView = AsyncCardFrame(parent.context)
        itemView.inflateAsync(R.layout.item_playing_card)
        return PlayingCardViewHolder(
            itemView,
            coroutineScope,
            playingCardController,
            cardAppearance
        )
    }

    override fun onBindViewHolder(viewHolder: PlayingCardViewHolder, position: Int) {
        val playingCard: PlayingCard = items[position]
        viewHolder.bind(playingCard)
    }

    override fun getItemCount(): Int = items.size
}
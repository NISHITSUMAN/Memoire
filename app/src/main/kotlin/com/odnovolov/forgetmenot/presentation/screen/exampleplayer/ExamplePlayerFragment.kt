package com.odnovolov.forgetmenot.presentation.screen.exampleplayer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.view.children
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.odnovolov.forgetmenot.R
import com.odnovolov.forgetmenot.domain.interactor.autoplay.PlayingCard
import com.odnovolov.forgetmenot.presentation.common.*
import com.odnovolov.forgetmenot.presentation.common.SpeakerImpl.Event.CannotGainAudioFocus
import com.odnovolov.forgetmenot.presentation.common.SpeakerImpl.Event.SpeakError
import com.odnovolov.forgetmenot.presentation.common.base.BaseFragment
import com.odnovolov.forgetmenot.presentation.screen.exampleplayer.ExamplePlayerController.Command.SetCurrentPosition
import com.odnovolov.forgetmenot.presentation.screen.exampleplayer.ExamplePlayerController.Command.ShowCannotGetAudioFocusMessage
import com.odnovolov.forgetmenot.presentation.screen.exampleplayer.ExamplePlayerEvent.*
import com.odnovolov.forgetmenot.presentation.screen.exercise.ReasonForInabilityToSpeak
import com.odnovolov.forgetmenot.presentation.screen.exercise.ReasonForInabilityToSpeak.*
import com.odnovolov.forgetmenot.presentation.screen.exercise.SpeakingStatus
import com.odnovolov.forgetmenot.presentation.screen.exercise.SpeakingStatus.*
import com.odnovolov.forgetmenot.presentation.screen.player.view.PlayerViewModel
import com.odnovolov.forgetmenot.presentation.screen.player.view.PlayingCardAdapter
import kotlinx.android.synthetic.main.fragment_example_player.*
import kotlinx.android.synthetic.main.popup_speak_error.view.*
import kotlinx.coroutines.launch

class ExamplePlayerFragment : BaseFragment() {
    init {
        ExamplePlayerDiScope.reopenIfClosed()
    }

    private var controller: ExamplePlayerController? = null
    private lateinit var viewModel: PlayerViewModel
    private val toast: Toast by lazy { Toast.makeText(requireContext(), "", Toast.LENGTH_SHORT) }
    private var speakErrorPopup: PopupWindow? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_example_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        viewCoroutineScope!!.launch {
            val diScope = ExamplePlayerDiScope.getAsync() ?: return@launch
            controller = diScope.controller
            viewModel = diScope.viewModel
            playerViewPager.adapter = PlayingCardAdapter(
                viewCoroutineScope!!,
                diScope.playingCardController,
                diScope.cardAppearance
            )
            observeViewModel()
            controller!!.commands.observe(::executeCommand)
        }
    }

    private fun setupView() {
        playerViewPager.offscreenPageLimit = 1
        playerViewPager.children.find { it is RecyclerView }?.let {
            (it as RecyclerView).isNestedScrollingEnabled = false
        }
        playerViewPager.registerOnPageChangeCallback(onPageChangeCallback)
    }

    private fun observeViewModel() {
        with (viewModel) {
            playingCards.observe { playingCards: List<PlayingCard> ->
                val adapter = playerViewPager.adapter as PlayingCardAdapter
                adapter.items = playingCards
                if (playerViewPager.currentItem != currentPosition) {
                    playerViewPager.setCurrentItem(currentPosition, false)
                }
                progressBar.visibility = View.GONE
            }
            hasPlayingCards.observe { hasPlayingCards: Boolean ->
                if (!hasPlayingCards) {
                    speakFrame.isVisible = false
                    emptyCardView.isVisible = true
                }
            }
            speakingStatus.observe { speakingStatus: SpeakingStatus ->
                with(speakButton) {
                    setImageResource(
                        when (speakingStatus) {
                            Speaking -> R.drawable.ic_round_volume_off_24
                            NotSpeaking -> R.drawable.ic_round_volume_up_24
                            CannotSpeak -> R.drawable.ic_volume_error_24
                        }
                    )
                    val iconTintRes: Int =
                        when (speakingStatus) {
                            CannotSpeak -> R.color.issue
                            else -> R.color.icon_on_control_panel
                        }
                    setTintFromRes(iconTintRes)
                    setOnClickListener {
                        when (speakingStatus) {
                            Speaking -> controller?.dispatch(StopSpeakButtonClicked)
                            NotSpeaking -> controller?.dispatch(SpeakButtonClicked)
                            CannotSpeak -> showSpeakErrorPopup()
                        }
                    }
                    contentDescription = getString(
                        when (speakingStatus) {
                            Speaking -> R.string.description_stop_speaking_button
                            NotSpeaking -> R.string.description_speak_button
                            CannotSpeak -> R.string.description_cannot_speak_button
                        }
                    )
                    setTooltipTextFromContentDescription()
                    uncover()
                }
            }
            isSpeakerPreparingToPronounce.observe { isPreparing: Boolean ->
                speakProgressBar.isInvisible = !isPreparing
            }
            speakerEvents.observe { event: SpeakerImpl.Event ->
                when (event) {
                    SpeakError -> toast.run {
                        setText(R.string.error_message_failed_to_speak)
                        show()
                    }
                    CannotGainAudioFocus -> toast.run {
                        setText(R.string.error_message_cannot_get_audio_focus)
                        show()
                    }
                }
            }
            isPlaying.observe { isPlaying: Boolean ->
                with(playButton) {
                    keepScreenOn = isPlaying
                    setImageResource(
                        if (isPlaying)
                            R.drawable.ic_pause_28 else
                            R.drawable.ic_play_28
                    )
                    setOnClickListener {
                        controller?.dispatch(
                            if (isPlaying)
                                PauseButtonClicked else
                                ResumeButtonClicked
                        )
                    }
                    contentDescription = getString(
                        if (isPlaying)
                            R.string.description_pause_button else
                            R.string.description_resume_button
                    )
                    setTooltipTextFromContentDescription()
                }
            }
        }
    }

    private fun executeCommand(command: ExamplePlayerController.Command) {
        when (command) {
            is SetCurrentPosition -> {
                playerViewPager.currentItem = command.position
            }
            ShowCannotGetAudioFocusMessage -> {
                toast.run {
                    setText(R.string.error_message_cannot_get_audio_focus)
                    show()
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun notifyBottomSheetStateChanged(newState: Int) {
        when (newState) {
            BottomSheetBehavior.STATE_EXPANDED -> {
                blocker.setOnTouchListener(null)
                controller?.dispatch(BottomSheetExpanded)
            }
            BottomSheetBehavior.STATE_COLLAPSED -> {
                blocker.setOnTouchListener { _, _ -> true }
                controller?.dispatch(BottomSheetCollapsed)
            }
        }
    }

    fun notifyBottomSheetSlideOffsetChanged(slideOffset: Float) {
        exampleTextView.alpha = 1f - slideOffset
    }

    private fun requireSpeakErrorPopup(): PopupWindow {
        if (speakErrorPopup == null) {
            val content = View.inflate(requireContext(), R.layout.popup_speak_error, null).apply {
                goToTtsSettingsButton.setOnClickListener {
                    openTtsSettings()
                    speakErrorPopup?.dismiss()
                }
            }
            speakErrorPopup = DarkPopupWindow(content)
            subscribeSpeakErrorPopup()
        }
        return speakErrorPopup!!
    }

    private fun subscribeSpeakErrorPopup() {
        viewCoroutineScope!!.launch {
            val diScope = ExamplePlayerDiScope.getAsync() ?: return@launch
            diScope.viewModel.reasonForInabilityToSpeak.observe { reason: ReasonForInabilityToSpeak? ->
                if (reason == null) {
                    speakErrorPopup?.dismiss()
                } else {
                    speakErrorPopup?.contentView?.run {
                        speakErrorDescriptionTextView.text =
                            composeSpeakErrorDescription(reason, requireContext())
                    }
                }
            }
        }
    }

    private fun showSpeakErrorPopup() {
        requireSpeakErrorPopup().show(anchor = speakButton, gravity = Gravity.BOTTOM)
    }

    override fun onPause() {
        super.onPause()
        viewCoroutineScope!!.launch {
            val diScope = ExamplePlayerDiScope.getAsync() ?: return@launch
            diScope.controller.dispatch(FragmentPaused)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        playerViewPager.adapter = null
        playerViewPager.unregisterOnPageChangeCallback(onPageChangeCallback)
        speakErrorPopup?.dismiss()
        speakErrorPopup = null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing()) {
            ExamplePlayerDiScope.close()
        }
    }

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            controller?.dispatch(PageWasChanged(position))
        }
    }
}
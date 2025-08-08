package com.odnovolov.forgetmenot.presentation.screen.pronunciationplan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.odnovolov.forgetmenot.R
import com.odnovolov.forgetmenot.presentation.common.addBottomSheetCallbackWithInitialNotification
import com.odnovolov.forgetmenot.presentation.common.base.BaseFragment
import com.odnovolov.forgetmenot.presentation.common.mainactivity.MainActivity
import com.odnovolov.forgetmenot.presentation.common.isFinishing
import com.odnovolov.forgetmenot.presentation.common.showToast
import com.odnovolov.forgetmenot.presentation.screen.deckeditor.decksettings.Tip
import com.odnovolov.forgetmenot.presentation.screen.exampleplayer.ExamplePlayerFragment
import com.odnovolov.forgetmenot.presentation.screen.pronunciationplan.PronunciationPlanController.Command.ShowCannotChangeLastSpeakAnswerMessage
import com.odnovolov.forgetmenot.presentation.screen.pronunciationplan.PronunciationPlanController.Command.ShowCannotChangeLastSpeakQuestionMessage
import com.odnovolov.forgetmenot.presentation.screen.pronunciationplan.PronunciationPlanUiEvent.*
import kotlinx.android.synthetic.main.fragment_pronunciation_plan.*
import kotlinx.android.synthetic.main.tip.*
import kotlinx.android.synthetic.main.tip.view.*
import kotlinx.coroutines.launch

class PronunciationPlanFragment : BaseFragment() {
    init {
        PronunciationPlanDiScope.reopenIfClosed()
    }

    private var controller: PronunciationPlanController? = null
    private lateinit var viewModel: PronunciationPlanViewModel
    private lateinit var pronunciationEventAdapter: PronunciationEventAdapter
    private lateinit var exampleFragment: ExamplePlayerFragment

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pronunciation_plan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        viewCoroutineScope!!.launch {
            val diScope = PronunciationPlanDiScope.getAsync() ?: return@launch
            controller = diScope.controller
            viewModel = diScope.viewModel
            pronunciationEventAdapter = PronunciationEventAdapter(controller!!)
            setupPronunciationPlanRecyclerView()
            observeViewModel()
            controller!!.commands.observe(::executeCommand)
        }
    }

    private fun setupView() {
        exampleFragment = childFragmentManager.findFragmentByTag("ExamplePlayerFragment")
                as ExamplePlayerFragment
        backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
        helpButton.setOnClickListener {
            controller?.dispatch(HelpButtonClicked)
        }
        addPronunciationEventButton.setOnClickListener {
            controller?.dispatch(AddPronunciationEventButtonClicked)
        }
    }

    private fun setupPronunciationPlanRecyclerView() {
        pronunciationPlanRecyclerView.adapter = pronunciationEventAdapter
        val itemTouchHelperCallback = PronunciationEventItemTouchHelperCallback(
            controller!!,
            pronunciationEventAdapter
        )
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        pronunciationEventAdapter.itemTouchHelper = itemTouchHelper
        itemTouchHelper.attachToRecyclerView(pronunciationPlanRecyclerView)
    }

    private fun observeViewModel() {
        with(viewModel) {
            tip.observe { tip: Tip? ->
                if (tip != null) {
                    if (tipStub != null) {
                        tipStub.inflate()
                        closeTipButton.setOnClickListener {
                            controller?.dispatch(CloseTipButtonClicked)
                        }
                    }
                    val tipLayout = rootView.findViewById<View>(R.id.tipLayout)
                    tipLayout.tipTextView.setText(tip.stringId)
                    tipLayout.isVisible = true
                } else {
                    if (tipStub == null) {
                        val tipLayout = rootView.findViewById<View>(R.id.tipLayout)
                        tipLayout.isVisible = false
                    }
                }
            }
            pronunciationEventItems.observe(pronunciationEventAdapter::setItems)
        }
    }

    private fun executeCommand(command: PronunciationPlanController.Command) {
        when (command) {
            ShowCannotChangeLastSpeakQuestionMessage ->
                showToast(R.string.error_message_cannot_change_last_speak_question)
            ShowCannotChangeLastSpeakAnswerMessage ->
                showToast(R.string.error_message_cannot_change_last_speak_answer)
        }
    }

    override fun onResume() {
        super.onResume()
        appBar.post { appBar.isActivated = contentScrollView.canScrollVertically(-1) }
        contentScrollView.viewTreeObserver.addOnScrollChangedListener(scrollListener)
        exampleFragmentContainerView
            .addBottomSheetCallbackWithInitialNotification(bottomSheetCallback)
        (activity as MainActivity).registerBackPressInterceptor(backPressInterceptor)
    }

    override fun onPause() {
        super.onPause()
        contentScrollView.viewTreeObserver.removeOnScrollChangedListener(scrollListener)
        val behavior = BottomSheetBehavior.from(exampleFragmentContainerView)
        behavior.removeBottomSheetCallback(bottomSheetCallback)
        (activity as MainActivity).unregisterBackPressInterceptor(backPressInterceptor)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pronunciationPlanRecyclerView.adapter = null
        pronunciationEventAdapter.itemTouchHelper = null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing()) {
            PronunciationPlanDiScope.close()
        }
    }

    val scrollListener = ViewTreeObserver.OnScrollChangedListener {
        val canScrollUp = contentScrollView.canScrollVertically(-1)
        if (appBar.isActivated != canScrollUp) {
            appBar.isActivated = canScrollUp
        }
    }

    private val bottomSheetCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            exampleFragment.notifyBottomSheetStateChanged(newState)
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            exampleFragment.notifyBottomSheetSlideOffsetChanged(slideOffset)
            screenFrame.alpha = 1f - slideOffset
        }
    }

    private val backPressInterceptor = MainActivity.BackPressInterceptor {
        val behavior = BottomSheetBehavior.from(exampleFragmentContainerView)
        if (behavior.state != BottomSheetBehavior.STATE_COLLAPSED) {
            behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            true
        } else {
            false
        }
    }
}
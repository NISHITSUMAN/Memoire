package com.odnovolov.forgetmenot.presentation.screen.settings

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import com.odnovolov.forgetmenot.R
import com.odnovolov.forgetmenot.presentation.common.base.BaseFragment
import com.odnovolov.forgetmenot.presentation.common.customview.ChoiceDialogCreator
import com.odnovolov.forgetmenot.presentation.common.customview.ChoiceDialogCreator.Item
import com.odnovolov.forgetmenot.presentation.common.customview.ChoiceDialogCreator.ItemAdapter
import com.odnovolov.forgetmenot.presentation.common.customview.ChoiceDialogCreator.ItemForm.AsCheckBox
import com.odnovolov.forgetmenot.presentation.common.customview.ChoiceDialogCreator.ItemForm.AsRadioButton
import com.odnovolov.forgetmenot.presentation.common.entity.FullscreenPreference
import com.odnovolov.forgetmenot.presentation.common.mainactivity.MainActivity
import com.odnovolov.forgetmenot.presentation.common.isFinishing
import com.odnovolov.forgetmenot.presentation.common.setDrawableStart
import com.odnovolov.forgetmenot.presentation.screen.settings.SettingsEvent.*
import com.odnovolov.forgetmenot.presentation.screen.settings.ThemeHelper.Theme
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.reflect.KProperty1

class SettingsFragment : BaseFragment() {
    init {
        SettingsDiScope.reopenIfClosed()
    }

    private var controller: SettingsController? = null
    private lateinit var fullscreenModeDialog: Dialog
    private lateinit var fullscreenPreferenceAdapter: ItemAdapter
    private lateinit var themeDialog: Dialog
    private lateinit var themeAdapter: ItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initFullscreenModeDialog()
        initThemeDialog()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    private fun initFullscreenModeDialog() {
        fullscreenModeDialog = ChoiceDialogCreator.create(
            context = requireContext(),
            itemForm = AsCheckBox,
            takeTitle = { titleTextView: TextView ->
                titleTextView.setText(R.string.title_fullscreen_mode_dialog)
                titleTextView.setDrawableStart(
                    R.drawable.ic_round_fullscreen_24,
                    R.color.title_icon_in_dialog
                )
            },
            onItemClick = { item: Item ->
                item as FullscreenPreferenceItem
                controller?.dispatch(
                    when (item.property) {
                        FullscreenPreference::isEnabledInExercise ->
                            FullscreenInExerciseCheckboxClicked
                        FullscreenPreference::isEnabledInCardPlayer ->
                            FullscreenInRepetitionCheckboxClicked
                        FullscreenPreference::isEnabledInOtherPlaces ->
                            FullscreenInOtherPlacesCheckboxClicked
                        else -> throw AssertionError()
                    }
                )
            },
            takeAdapter = { fullscreenPreferenceAdapter = it }
        )
    }

    private fun initThemeDialog() {
        themeDialog = ChoiceDialogCreator.create(
            context = requireContext(),
            itemForm = AsRadioButton,
            takeTitle = { titleTextView: TextView ->
                titleTextView.setText(R.string.title_theme_dialog)
                titleTextView.setDrawableStart(
                    R.drawable.ic_round_brightness_medium_24,
                    R.color.title_icon_in_dialog
                )
            },
            onItemClick = { item: Item ->
                item as ThemeItem
                ThemeHelper.applyTheme(item.theme, requireContext())
                themeDialog.dismiss()
            },
            takeAdapter = { themeAdapter = it }
        )
    }

    @ExperimentalStdlibApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        viewCoroutineScope!!.launch {
            val diScope = SettingsDiScope.getAsync() ?: return@launch
            controller = diScope.controller
            observeViewModel(diScope.viewModel)
        }
    }

    private fun setupView() {
        backButton.setOnClickListener {
            requireActivity().onBackPressed()
        }
        fullscreenButton.setOnClickListener {
            fullscreenModeDialog.show()
        }
        themeButton.setOnClickListener {
            themeDialog.show()
        }
        cardAppearanceButton.setOnClickListener {
            controller?.dispatch(CardAppearanceButtonClicked)
        }
        exerciseButton.setOnClickListener {
            controller?.dispatch(ExerciseButtonClicked)
        }
        walkingModeButton.setOnClickListener {
            controller?.dispatch(WalkingModeButtonClicked)
        }
    }

    private fun observeViewModel(viewModel: SettingsViewModel) {
        with(viewModel) {
            fullscreenPreference.observe { fullscreenPreference: FullscreenPreference ->
                (requireActivity() as MainActivity).fullscreenModeManager
                    ?.setFullscreenMode(fullscreenPreference.isEnabledInOtherPlaces)

                val items = listOf(
                    FullscreenPreferenceItem(
                        property = FullscreenPreference::isEnabledInExercise,
                        text = getString(R.string.item_text_fullscreen_in_exercise),
                        isSelected = fullscreenPreference.isEnabledInExercise
                    ),
                    FullscreenPreferenceItem(
                        property = FullscreenPreference::isEnabledInCardPlayer,
                        text = getString(R.string.item_text_fullscreen_in_card_player),
                        isSelected = fullscreenPreference.isEnabledInCardPlayer
                    ),
                    FullscreenPreferenceItem(
                        property = FullscreenPreference::isEnabledInOtherPlaces,
                        text = getString(R.string.item_text_fullscreen_in_other_places),
                        isSelected = fullscreenPreference.isEnabledInOtherPlaces
                    )
                )
                fullscreenPreferenceAdapter.submitList(items)

                fullscreenSettingsDescription.text = with(fullscreenPreference) {
                    when {
                        isEnabledInOtherPlaces
                                && isEnabledInExercise
                                && isEnabledInCardPlayer -> getString(R.string.everywhere)
                        !isEnabledInOtherPlaces
                                && !isEnabledInExercise
                                && !isEnabledInCardPlayer -> getString(R.string.nowhere)
                        else -> {
                            items.filter { item: Item -> item.isSelected }
                                .joinToString { item: Item ->
                                    item.text.toLowerCase(Locale.ROOT)
                                }.run { capitalize(Locale.ROOT) }
                        }
                    }
                }
            }
        }
        ThemeHelper.state.flowOf(ThemeHelper.State::currentTheme).observe { currentTheme: Theme ->
            themeDescription.setText(currentTheme.stringRes)
            val items: List<ThemeItem> = Theme.values().map { theme: Theme ->
                ThemeItem(
                    theme,
                    text = getString(theme.stringRes),
                    isSelected = theme == currentTheme
                )
            }
            themeAdapter.submitList(items)
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.run {
            getBundle(STATE_FULLSCREEN_MODE_DIALOG)
                ?.let(fullscreenModeDialog::onRestoreInstanceState)
            getBundle(STATE_THEME_DIALOG)
                ?.let(themeDialog::onRestoreInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle(STATE_FULLSCREEN_MODE_DIALOG, fullscreenModeDialog.onSaveInstanceState())
        outState.putBundle(STATE_THEME_DIALOG, themeDialog.onSaveInstanceState())
    }

    override fun onResume() {
        super.onResume()
        appBar.post { appBar.isActivated = contentScrollView.canScrollVertically(-1) }
        contentScrollView.viewTreeObserver.addOnScrollChangedListener(scrollListener)
    }

    override fun onPause() {
        super.onPause()
        contentScrollView.viewTreeObserver.removeOnScrollChangedListener(scrollListener)
    }

    private val scrollListener = ViewTreeObserver.OnScrollChangedListener {
        val canScrollUp = contentScrollView.canScrollVertically(-1)
        if (appBar.isActivated != canScrollUp) {
            appBar.isActivated = canScrollUp
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing()) {
            SettingsDiScope.close()
        }
    }

    data class FullscreenPreferenceItem(
        val property: KProperty1<FullscreenPreference, Boolean>,
        override val text: String,
        override val isSelected: Boolean
    ) : Item

    data class ThemeItem(
        val theme: Theme,
        override val text: String,
        override val isSelected: Boolean
    ) : Item

    companion object {
        private const val STATE_FULLSCREEN_MODE_DIALOG = "STATE_FULLSCREEN_MODE_DIALOG"
        private const val STATE_THEME_DIALOG = "STATE_THEME_DIALOG"
    }
}
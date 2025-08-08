package com.odnovolov.forgetmenot.presentation.screen.deckeditor.decksettings

import com.odnovolov.forgetmenot.domain.entity.*
import com.odnovolov.forgetmenot.domain.interactor.decksettings.DeckSettings
import com.odnovolov.forgetmenot.presentation.screen.deckeditor.decksettings.preset.Preset
import com.odnovolov.forgetmenot.presentation.screen.deckeditor.decksettings.preset.PresetDialogState
import com.odnovolov.forgetmenot.presentation.screen.deckeditor.decksettings.preset.SkeletalPresetViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class ExercisePreferencePresetViewModel(
    private val presetDialogState: PresetDialogState,
    private val deckSettingsState: DeckSettings.State,
    private val globalState: GlobalState
) : SkeletalPresetViewModel() {
    override val availablePresets: Flow<List<Preset>> = combine(
        deckSettingsState.deck.flowOf(Deck::exercisePreference),
        globalState.flowOf(GlobalState::sharedExercisePreferences)
    ) { currentExercisePreference: ExercisePreference,
        sharedExercisePreferences: Collection<ExercisePreference>
        ->
        (sharedExercisePreferences + currentExercisePreference + ExercisePreference.Default)
            .distinctBy { it.id }
    }
        .flatMapLatest { exercisePreferences: List<ExercisePreference> ->
            val exercisePreferenceNameFlows: List<Flow<String>> = exercisePreferences
                .map { it.flowOf(ExercisePreference::name) }
            combine(exercisePreferenceNameFlows) {
                val currentExercisePreference = deckSettingsState.deck.exercisePreference
                exercisePreferences.map { exercisePreference: ExercisePreference ->
                    Preset(
                        id = exercisePreference.id,
                        name = exercisePreference.name,
                        isSelected = exercisePreference.id == currentExercisePreference.id
                    )
                }
                    .sortedWith(compareBy({ it.name }, { it.id }))
            }
        }

    override val presetInputCheckResult: Flow<NameCheckResult> = presetDialogState
        .flowOf(PresetDialogState::typedPresetName)
        .map { typedPresetName: String ->
            checkExercisePreferenceName(typedPresetName, globalState)
        }

    override val deckNamesThatUsePreset: Flow<List<String>> = presetDialogState
        .flowOf(PresetDialogState::idToDelete)
        .map { sharedExercisePreferenceIdToDelete ->
            globalState.decks
                .filter { deck: Deck ->
                    deck.exercisePreference.id == sharedExercisePreferenceIdToDelete
                }
                .map { deck: Deck -> deck.name }
        }

    override val presetNameToDelete: String?
        get() = globalState.sharedExercisePreferences
            .find { exercisePreference: ExercisePreference ->
                exercisePreference.id == presetDialogState.idToDelete
            }
            ?.name
}
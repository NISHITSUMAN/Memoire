package com.odnovolov.forgetmenot.presentation.screen.deckeditor.decksettings.preset

import android.content.Context
import com.odnovolov.forgetmenot.R.string

data class Preset(
    val id: Long?,
    val name: String,
    val isSelected: Boolean
)

fun Preset.isOff(): Boolean = id == null
fun Preset.isDefault(): Boolean = id == 0L
fun Preset.isIndividual(): Boolean = !isOff() && !isDefault() && name.isEmpty()
fun Preset.isShared(): Boolean = name.isNotEmpty()

fun Preset.toString(context: Context): String = when {
    isOff() -> context.getString(string.off)
    isDefault() -> context.getString(string.preset_name_default)
    isIndividual() -> context.getString(string.preset_name_individual)
    else -> "'${name}'"
}
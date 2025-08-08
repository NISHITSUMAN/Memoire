package com.odnovolov.forgetmenot.presentation.screen.intervals

import android.content.Context
import com.odnovolov.forgetmenot.R
import com.odnovolov.forgetmenot.domain.architecturecomponents.FlowMaker
import com.odnovolov.forgetmenot.domain.toDateTimeSpan
import com.odnovolov.forgetmenot.presentation.screen.intervals.DisplayedInterval.IntervalUnit.*
import com.odnovolov.forgetmenot.presentation.screen.intervals.DisplayedInterval.IntervalUnit.Months
import com.soywiz.klock.*

class DisplayedInterval(
    value: Int?,
    intervalUnit: IntervalUnit
): FlowMaker<DisplayedInterval>() {
    var value: Int? by flowMaker(value)
    var intervalUnit: IntervalUnit by flowMaker(intervalUnit)

    fun isValid(): Boolean = value.let { value -> value != null && value > 0 }

    fun toDateTimeSpan(): DateTimeSpan {
        if (!isValid()) throw IllegalStateException("intervalNumber is not valid")
        return when (intervalUnit) {
            Minutes -> value!!.minutes.toDateTimeSpan()
            Hours -> value!!.hours.toDateTimeSpan()
            Days -> value!!.days.toDateTimeSpan()
            Months -> value!!.months.toDateTimeSpan()
        }
    }

    fun toString(context: Context): String {
        val pluralsId: Int = when (intervalUnit) {
            Minutes -> R.plurals.interval_unit_minutes
            Hours -> R.plurals.interval_unit_hours
            Days -> R.plurals.interval_unit_days
            Months -> R.plurals.interval_unit_months
        }
        val quantity: Int = value ?: 0
        val intervalUnit: String = context.resources.getQuantityString(pluralsId, quantity)
        return "$value $intervalUnit"
    }

    fun getAbbreviation(context: Context): String {
        val intervalUnitAbbreviation: String = context.getString(
            when (intervalUnit) {
                Minutes -> R.string.interval_unit_abbreviation_minutes
                Hours -> R.string.interval_unit_abbreviation_hours
                Days -> R.string.interval_unit_abbreviation_days
                Months -> R.string.interval_unit_abbreviation_months
            }
        )
        return value.toString() + intervalUnitAbbreviation
    }

    companion object {
        fun fromDateTimeSpan(dateTimeSpan: DateTimeSpan): DisplayedInterval {
            return when {
                dateTimeSpan.monthSpan.totalMonths != 0 -> {
                    DisplayedInterval(
                        value = dateTimeSpan.monthSpan.totalMonths,
                        intervalUnit = Months
                    )
                }
                dateTimeSpan.timeSpan % 1.days == TimeSpan.ZERO -> {
                    DisplayedInterval(
                        value = dateTimeSpan.timeSpan.days.toInt(),
                        intervalUnit = Days
                    )
                }
                dateTimeSpan.timeSpan % 1.hours == TimeSpan.ZERO -> {
                    DisplayedInterval(
                        value = dateTimeSpan.timeSpan.hours.toInt(),
                        intervalUnit = Hours
                    )
                }
                else -> {
                    DisplayedInterval(
                        value = dateTimeSpan.timeSpan.minutes.toInt(),
                        intervalUnit = Minutes
                    )
                }
            }
        }
    }

    enum class IntervalUnit {
        Minutes,
        Hours,
        Days,
        Months
    }
}
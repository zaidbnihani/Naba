package com.nba.plus.ui.util

import android.content.Context
import com.nba.plus.R
import java.time.Duration
import java.time.Instant
import java.util.Locale

object TimeFormat {

    /** وقت نسبي عربي: الآن / منذ ٥ دقائق / منذ ساعتين… */
    fun relative(context: Context, instant: Instant): String {
        val minutes = Duration.between(instant, Instant.now()).toMinutes()
        return when {
            minutes < 1 -> context.getString(R.string.time_now)
            minutes < 60 -> context.resources.getQuantityString(
                R.plurals.time_minutes_ago, minutes.toInt(), minutes,
            )
            minutes < 60 * 24 -> {
                val hours = minutes / 60
                context.resources.getQuantityString(
                    R.plurals.time_hours_ago, hours.toInt(), hours,
                )
            }
            else -> {
                val days = minutes / (60 * 24)
                context.resources.getQuantityString(
                    R.plurals.time_days_ago, days.toInt(), days,
                )
            }
        }
    }

    /** عدد مضغوط: 1.2K / 3.4M. */
    fun compact(number: Long): String = when {
        number >= 1_000_000 -> String.format(Locale.US, "%.1fM", number / 1_000_000.0)
        number >= 1_000 -> String.format(Locale.US, "%.1fK", number / 1_000.0)
        else -> number.toString()
    }
}

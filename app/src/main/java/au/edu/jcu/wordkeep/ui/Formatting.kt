package au.edu.jcu.wordkeep.ui

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** Human-friendly dates: "today", "tomorrow", then "Fri 27 Nov". */
fun LocalDate.friendly(today: LocalDate): String = when (this) {
    today -> "today"
    today.plusDays(1) -> "tomorrow"
    else -> format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))
}

fun LocalDate.longDate(): String = format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault()))

fun LocalDate.shortDay(): String = dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())

fun plural(count: Int, singular: String, pluralForm: String = singular + "s"): String =
    "$count ${if (count == 1) singular else pluralForm}"

package au.edu.jcu.wordkeep.data.local.entity

import au.edu.jcu.wordkeep.domain.model.StudyMode
import java.time.LocalDate

/** Result rows of the aggregate statistics queries in [au.edu.jcu.wordkeep.data.local.dao.ReviewLogDao]. */
data class DayTotalsRow(val day: LocalDate, val answered: Int, val correct: Int)

data class ModeTotalsRow(val mode: StudyMode, val answered: Int, val correct: Int)

data class HardWordRow(val wordId: Long, val text: String, val attempts: Int, val correct: Int, val lapses: Int)

data class TotalsRow(val answered: Int, val correct: Int, val millis: Long)

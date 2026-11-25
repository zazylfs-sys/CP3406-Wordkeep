package au.edu.jcu.wordkeep.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import au.edu.jcu.wordkeep.domain.model.FetchState
import au.edu.jcu.wordkeep.domain.model.Grade
import au.edu.jcu.wordkeep.domain.model.StudyMode
import au.edu.jcu.wordkeep.domain.model.WordOrigin
import java.time.Instant
import java.time.LocalDate

/** A saved word. Dictionary content lives here and in [SenseEntity]; progress lives in [CardEntity]. */
@Entity(tableName = "words", indices = [Index(value = ["text"], unique = true)])
data class WordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val phonetic: String?,
    @ColumnInfo(name = "audio_urls") val audioUrls: List<String>,
    @ColumnInfo(name = "alternate_spellings") val alternateSpellings: List<String>,
    val origin: WordOrigin,
    @ColumnInfo(name = "fetch_state") val fetchState: FetchState,
    @ColumnInfo(name = "added_at") val addedAt: Instant,
    @ColumnInfo(name = "fetched_at") val fetchedAt: Instant?,
)

/** One numbered definition of a word. Deleted with its word. */
@Entity(
    tableName = "senses",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("word_id")],
)
data class SenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "word_id") val wordId: Long,
    val position: Int,
    @ColumnInfo(name = "part_of_speech") val partOfSpeech: String,
    val definition: String,
    val example: String?,
    val synonyms: List<String>,
)

/** SM-2 scheduling state, one row per word (the word id is the primary key). */
@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("due_day")],
)
data class CardEntity(
    @PrimaryKey @ColumnInfo(name = "word_id") val wordId: Long,
    val repetitions: Int,
    @ColumnInfo(name = "interval_days") val intervalDays: Int,
    @ColumnInfo(name = "ease_factor") val easeFactor: Double,
    @ColumnInfo(name = "due_day") val dueDay: LocalDate?,
    val lapses: Int,
    @ColumnInfo(name = "introduced_day") val introducedDay: LocalDate?,
    @ColumnInfo(name = "last_reviewed_at") val lastReviewedAt: Instant?,
) {
    companion object {
        fun newFor(wordId: Long) = CardEntity(
            wordId = wordId,
            repetitions = 0,
            intervalDays = 0,
            easeFactor = 2.5,
            dueDay = null,
            lapses = 0,
            introducedDay = null,
            lastReviewedAt = null,
        )
    }
}

/** Every answer the learner gives. The statistics screen is built from this table. */
@Entity(
    tableName = "review_log",
    foreignKeys = [
        ForeignKey(
            entity = WordEntity::class,
            parentColumns = ["id"],
            childColumns = ["word_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("word_id"), Index("answered_day")],
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "word_id") val wordId: Long,
    val mode: StudyMode,
    val grade: Grade?,
    val correct: Boolean,
    @ColumnInfo(name = "answered_at") val answeredAt: Instant,
    @ColumnInfo(name = "answered_day") val answeredDay: LocalDate,
    @ColumnInfo(name = "response_millis") val responseMillis: Long,
)

/** A word with its senses and (optional) card, loaded in one transaction. */
data class WordWithDetails(
    @Embedded val word: WordEntity,
    @Relation(parentColumn = "id", entityColumn = "word_id")
    val senses: List<SenseEntity>,
    @Relation(parentColumn = "id", entityColumn = "word_id")
    val card: CardEntity?,
)

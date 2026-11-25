package au.edu.jcu.wordkeep.data.local

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate

/** Room type converters. Dates are stored as numbers so SQL can compare and group them. */
class Converters {

    @TypeConverter
    fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    /** Epoch day (days since 1970-01-01) keeps "due today" and "group by day" queries simple. */
    @TypeConverter
    fun dateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun epochDayToDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    /** Lists are stored as JSON so that values containing commas survive the round trip. */
    @TypeConverter
    fun stringListToJson(value: List<String>): String = json.encodeToString(stringList, value)

    @TypeConverter
    fun jsonToStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else json.decodeFromString(stringList, value)

    private companion object {
        val json = Json
        val stringList = ListSerializer(String.serializer())
    }
}

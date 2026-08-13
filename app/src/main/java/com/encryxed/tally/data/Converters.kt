package com.encryxed.tally.data

import androidx.room.TypeConverter
import com.encryxed.tally.parse.Category
import java.time.LocalDate

class Converters {

    @TypeConverter
    fun toLocalDate(epochDay: Long?): LocalDate? = epochDay?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun toCategory(name: String?): Category? = name?.let {
        runCatching { Category.valueOf(it) }.getOrDefault(Category.OTHER)
    }

    @TypeConverter
    fun fromCategory(category: Category?): String? = category?.name
}

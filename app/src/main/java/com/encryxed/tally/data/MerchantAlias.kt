package com.encryxed.tally.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.encryxed.tally.parse.Category

/**
 * A shop name the user corrected by hand.
 *
 * This is what makes recognition improve with use: the parser's guess for a
 * given till is consistent, so once you fix it the app recognises that shop
 * on every later visit.
 */
@Entity(tableName = "merchant_aliases")
data class MerchantAlias(
    @PrimaryKey val signature: String,
    val merchant: String,
    val category: Category,
)

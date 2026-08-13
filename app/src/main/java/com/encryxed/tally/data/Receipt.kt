package com.encryxed.tally.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.encryxed.tally.parse.Category
import java.time.LocalDate

/** A saved receipt. Everything here lives only in the app's own database. */
@Entity(tableName = "receipts")
data class Receipt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
    val total: Double,
    val currency: String,
    /** Stored as an epoch day, so ORDER BY sorts chronologically. */
    val date: LocalDate,
    val category: Category,
    /** Absolute path to the captured photo inside the app's private storage. */
    val imagePath: String? = null,
    val note: String = "",
    /** Kept so a mis-parse can be re-read later without another photo. */
    val rawText: String = "",
    /**
     * Set when the parser wasn't confident about a field. Receipts save
     * instantly after the photo, so this is how the list flags the ones
     * worth going back and fixing.
     */
    val needsReview: Boolean = false,
    /**
     * Fingerprint of the till that produced this receipt. Editing the shop
     * name later uses it to teach the parser, exactly as confirming at scan
     * time used to.
     */
    val signature: String = "",
    val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        /** Placeholder used when the shop name could not be read at all. */
        const val UNKNOWN_MERCHANT = "Unknown store"
    }
}

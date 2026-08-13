package com.encryxed.tally.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Receipt::class, MerchantAlias::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class TallyDatabase : RoomDatabase() {

    abstract fun receiptDao(): ReceiptDao
    abstract fun merchantAliasDao(): MerchantAliasDao

    companion object {
        /**
         * Adds the learned-shop-names table. Written as a real migration
         * rather than a destructive one so receipts already saved on the
         * phone survive the upgrade.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `merchant_aliases` (
                        `signature` TEXT NOT NULL,
                        `merchant` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        PRIMARY KEY(`signature`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Receipts now save instantly after the photo, so each row records
         * whether the parser was unsure (to flag it in the list) and which
         * till it came from (so a later edit still teaches the parser).
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `receipts` ADD COLUMN `needsReview` INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE `receipts` ADD COLUMN `signature` TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        @Volatile
        private var instance: TallyDatabase? = null

        fun get(context: Context): TallyDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TallyDatabase::class.java,
                    "tally.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { instance = it }
            }
    }
}

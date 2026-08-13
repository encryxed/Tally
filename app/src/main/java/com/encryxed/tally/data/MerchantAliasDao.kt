package com.encryxed.tally.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MerchantAliasDao {

    @Query("SELECT * FROM merchant_aliases WHERE signature = :signature LIMIT 1")
    suspend fun forSignature(signature: String): MerchantAlias?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alias: MerchantAlias)

    @Query("SELECT COUNT(*) FROM merchant_aliases")
    suspend fun count(): Int
}

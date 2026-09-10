package com.devcris80.prototipo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ChapetaDao {
    @Insert
    suspend fun insert(chapeta: Chapeta)

    @Query("SELECT * FROM chapeta WHERE idChip = :idChip LIMIT 1")
    suspend fun findByIdChip(idChip: String): Chapeta?
}

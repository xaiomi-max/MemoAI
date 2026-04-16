package com.memoai.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {
    @Query("SELECT * FROM memos ORDER BY id DESC")
    fun observeMemos(): Flow<List<MemoEntity>>

    @Query("SELECT COUNT(*) FROM memos")
    suspend fun count(): Int

    @Insert
    suspend fun insert(entity: MemoEntity): Long

    @Update
    suspend fun update(entity: MemoEntity)

    @Delete
    suspend fun delete(entity: MemoEntity)
}

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

    @Query("SELECT * FROM memos WHERE remindAtMillis IS NOT NULL AND remindAtMillis > :now")
    suspend fun futureReminders(now: Long): List<MemoEntity>

    @Query("SELECT * FROM memos WHERE type = :type AND completed = 0 ORDER BY id DESC LIMIT :limit")
    suspend fun observeIncompleteTasks(type: String, limit: Int): List<MemoEntity>

    @Query("SELECT * FROM memos WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MemoEntity?

    @Query("UPDATE memos SET completed = :completed, completedAtMillis = :completedAtMillis WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean, completedAtMillis: Long?)

    @Insert
    suspend fun insert(entity: MemoEntity): Long

    @Update
    suspend fun update(entity: MemoEntity)

    @Delete
    suspend fun delete(entity: MemoEntity)
}

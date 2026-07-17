package com.memoai.app.data

import kotlinx.coroutines.flow.Flow

class MemoRepository(private val dao: MemoDao) {
    fun observeMemos(): Flow<List<MemoEntity>> = dao.observeMemos()

    suspend fun count(): Int = dao.count()

    suspend fun insert(entity: MemoEntity): Long = dao.insert(entity)

    suspend fun update(entity: MemoEntity) = dao.update(entity)

    suspend fun delete(entity: MemoEntity) = dao.delete(entity)

    suspend fun futureReminders(now: Long): List<MemoEntity> = dao.futureReminders(now)

    suspend fun getById(id: Long): MemoEntity? = dao.getById(id)

    suspend fun setTaskCompleted(id: Long, completed: Boolean) {
        val completedAtMillis = if (completed) System.currentTimeMillis() else null
        dao.setCompleted(id, completed, completedAtMillis)
    }
}

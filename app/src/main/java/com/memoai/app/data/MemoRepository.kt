package com.memoai.app.data

import kotlinx.coroutines.flow.Flow

class MemoRepository(private val dao: MemoDao) {
    fun observeMemos(): Flow<List<MemoEntity>> = dao.observeMemos()

    suspend fun count(): Int = dao.count()

    suspend fun insert(entity: MemoEntity): Long = dao.insert(entity)

    suspend fun update(entity: MemoEntity) = dao.update(entity)

    suspend fun delete(entity: MemoEntity) = dao.delete(entity)
}

package com.example.sunote1.data

import kotlinx.coroutines.flow.Flow

class NoteRepository(private val dao: NoteDao) {

    /** 검색어에 맞춰 Flow 제공 */
    fun searchFlow(query: String): Flow<List<Note>> =
        if (query.isBlank()) dao.observeAll()
        else dao.search("%$query%")  // ← 반드시 인자 전달

    /** 단건 조회 */
    suspend fun get(id: Long): Note? = dao.getById(id)

    /** 저장/갱신 */
    suspend fun upsert(note: Note) = dao.upsert(note)

    /** 삭제 */
    suspend fun delete(id: Long) = dao.deleteById(id)

    /** 백업 즉시 조회 */
    suspend fun getAllNow(): List<Note> = dao.getAll()

    /** 가져오기(교체) */
    suspend fun replaceAll(list: List<Note>) {
        dao.clearAll()
        dao.insertAll(list)
    }

    /** 가져오기(추가) */
    suspend fun insertAll(list: List<Note>) = dao.insertAll(list)
}
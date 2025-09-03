package com.example.sunote1.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    // 전체 목록 관찰 (검색어 없음)
    @Query("SELECT * FROM notes ORDER BY CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END, dueAt ASC, id DESC")
    fun observeAll(): Flow<List<Note>>

    // 검색(제목/내용)
    @Query("""
        SELECT * FROM notes
        WHERE title LIKE :q OR content LIKE :q
        ORDER BY CASE WHEN dueAt IS NULL THEN 1 ELSE 0 END, dueAt ASC, id DESC
    """)
    fun search(q: String): Flow<List<Note>>

    // 단건 조회
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Note?

    // 저장/갱신 (REPLACE = upsert 효과)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: Note): Long

    // ID로 삭제
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    // 백업/가져오기용
    @Query("SELECT * FROM notes")
    suspend fun getAll(): List<Note>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<Note>)

    @Query("DELETE FROM notes")
    suspend fun clearAll()
}
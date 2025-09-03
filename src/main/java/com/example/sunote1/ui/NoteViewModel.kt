package com.example.sunote1.ui

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sunote1.data.Note
import com.example.sunote1.data.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteViewModel(private val repo: NoteRepository) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    // 검색어에 따라 흐름 전환
    val notes: StateFlow<List<Note>> =
        query
            .debounce(150)
            .flatMapLatest { q -> repo.searchFlow(q) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(q: String) {
        viewModelScope.launch { _query.emit(q) }
    }

    suspend fun getNote(id: Long): Note? = repo.get(id)

    fun saveNote(id: Long, title: String, content: String, dueAt: Long?) {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val note = if (id > 0) {
                // 기존 값 유지용으로 get -> 필드 갱신
                (repo.get(id) ?: Note(id = id, title = "", content = "", createdAt = now)).copy(
                    title = title,
                    content = content,
                    dueAt = dueAt
                )
            } else {
                Note(title = title, content = content, createdAt = now, dueAt = dueAt)
            }
            repo.upsert(note)
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.delete(id)
        }
    }

    // ---------------------------
    // 백업/가져오기 (org.json 사용)
    // ---------------------------

    /** 자동 백업: Downloads/sunote1/ 에 저장 (Android 10+는 MediaStore, 그 이하는 경로로 저장) */
    fun exportToDownloads(
        context: Context,
        onDone: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val list = repo.getAllNow()
                val json = JSONArray().apply {
                    list.forEach { n ->
                        put(JSONObject().apply {
                            put("id", n.id)
                            put("title", n.title)
                            put("content", n.content)
                            put("createdAt", n.createdAt)
                            if (n.dueAt != null) put("dueAt", n.dueAt) else put("dueAt", JSONObject.NULL)
                        })
                    }
                }
                val bytes = json.toString(2).toByteArray(Charsets.UTF_8)
                val fileName = "sunote_backup_${
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                }.json"

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "application/json")
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/sunote1")
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                        ?: error("MediaStore에 파일을 만들 수 없습니다.")
                    resolver.openOutputStream(uri)?.use { it.write(bytes) }
                        ?: error("출력 스트림을 열 수 없습니다.")
                    "다운로드/sunote1/$fileName"
                } else {
                    @Suppress("DEPRECATION")
                    val base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val dir = File(base, "sunote1").apply { if (!exists()) mkdirs() }
                    val outFile = File(dir, fileName)
                    FileOutputStream(outFile).use { it.write(bytes) }
                    outFile.absolutePath
                }
            }

            withContext(Dispatchers.Main) {
                result.onSuccess { path -> onDone(true, "$path 에 저장됨") }
                    .onFailure { e -> onDone(false, e.message ?: "알 수 없는 오류") }
            }
        }
    }

    /** SAF로 넘어온 URI에 백업(수동 저장용이 필요할 때) */
    fun exportToUri(
        context: Context,
        uri: Uri,
        onDone: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val list = repo.getAllNow()
                val json = JSONArray().apply {
                    list.forEach { n ->
                        put(JSONObject().apply {
                            put("id", n.id)
                            put("title", n.title)
                            put("content", n.content)
                            put("createdAt", n.createdAt)
                            if (n.dueAt != null) put("dueAt", n.dueAt) else put("dueAt", JSONObject.NULL)
                        })
                    }
                }
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(json.toString(2).toByteArray(Charsets.UTF_8))
                } ?: error("출력 스트림을 열 수 없습니다.")
                "저장됨"
            }

            withContext(Dispatchers.Main) {
                result.onSuccess { msg -> onDone(true, msg) }
                    .onFailure { e -> onDone(false, e.message ?: "알 수 없는 오류") }
            }
        }
    }

    /** JSON을 읽어 DB에 반영 (replaceExisting=true면 전체 교체) */
    fun importFromUri(
        context: Context,
        uri: Uri,
        replaceExisting: Boolean,
        onDone: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val text = context.contentResolver.openInputStream(uri)?.use { ins ->
                    ins.readBytes().toString(Charsets.UTF_8)
                } ?: error("입력 스트림을 열 수 없습니다.")
                val arr = JSONArray(text)
                val list = mutableListOf<Note>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        Note(
                            id = if (o.has("id")) o.optLong("id", 0L) else 0L,
                            title = o.optString("title", ""),
                            content = o.optString("content", ""),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                            dueAt = if (o.isNull("dueAt")) null else o.optLong("dueAt")
                        )
                    )
                }
                if (replaceExisting) repo.replaceAll(list) else repo.insertAll(list)
                "메모 ${list.size}건"
            }

            withContext(Dispatchers.Main) {
                result.onSuccess { msg -> onDone(true, msg) }
                    .onFailure { e -> onDone(false, e.message ?: "알 수 없는 오류") }
            }
        }
    }
}
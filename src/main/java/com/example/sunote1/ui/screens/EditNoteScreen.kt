package com.example.sunote1.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.sunote1.ui.NoteViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditNoteScreen(
    viewModel: NoteViewModel,
    noteId: Long,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var dueAt by remember { mutableStateOf<Long?>(null) }

    // 기존 노트 로드
    LaunchedEffect(noteId) {
        if (noteId > 0) {
            viewModel.getNote(noteId)?.let {
                title = it.title
                content = it.content
                dueAt = it.dueAt
            }
        }
    }

    val fmt = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text(if (noteId > 0) "메모 수정" else "새 메모") },
                actions = {

                    // 기존 노트일 때만 삭제 아이콘
                    if (noteId > 0) {
                        IconButton(onClick = {
                            scope.launch {
                                viewModel.deleteNote(noteId)
                                onClose()
                            }
                        }) {
                            Icon(Icons.Filled.Delete, contentDescription = "삭제")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("제목") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("내용") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )

            Spacer(Modifier.height(12.dp))

            // 일정 설정 / 해제
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    // 날짜 → 시간 순서로 받기
                    val cal = Calendar.getInstance()
                    DatePickerDialog(
                        ctx,
                        { _, y, m, d ->
                            val timeCal = Calendar.getInstance()
                            TimePickerDialog(
                                ctx,
                                { _, hh, mm ->
                                    val final = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, y)
                                        set(Calendar.MONTH, m)
                                        set(Calendar.DAY_OF_MONTH, d)
                                        set(Calendar.HOUR_OF_DAY, hh)
                                        set(Calendar.MINUTE, mm)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }.timeInMillis
                                    dueAt = final
                                },
                                timeCal.get(Calendar.HOUR_OF_DAY),
                                timeCal.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }) { Text("일정설정") }

                OutlinedButton(onClick = { dueAt = null }) {
                    Text("일정해제")
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("일정: " + (dueAt?.let { fmt.format(Date(it)) } ?: "없음"))

            Spacer(Modifier.height(20.dp))

            // 저장 / 닫기
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch {
                        viewModel.saveNote(noteId, title, content, dueAt)
                        onClose()
                    }
                }) { Text("저장") }

                OutlinedButton(onClick = onClose) { Text("닫기") }
            }
        }
    }
}
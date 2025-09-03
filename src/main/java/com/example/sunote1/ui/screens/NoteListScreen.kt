package com.example.sunote1.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.sunote1.data.Note
import com.example.sunote1.ui.NoteViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteListScreen(
    viewModel: NoteViewModel,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val notes by viewModel.notes.collectAsState()
    val query by viewModel.query.collectAsState()

    // 단일 선택 상태
    var selectedNoteId by remember { mutableStateOf<Long?>(null) }
    fun selectedNote(): Note? = notes.firstOrNull { it.id == selectedNoteId }

    // 가져오기(SAF)
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importFromUri(
                context = ctx,
                uri = uri,
                replaceExisting = true,
                onDone = { ok, msg ->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (ok) "가져오기 완료: $msg" else "가져오기 실패: $msg"
                        )
                    }
                }
            )
        } else {
            scope.launch { snackbarHostState.showSnackbar("가져오기가 취소되었습니다.") }
        }
    }

    val fmt = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    val now = remember { System.currentTimeMillis() }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    val sel = selectedNote()
                    if (sel == null) {
                        Text("SuNote1")
                    } else {
                        Text("선택됨: " + (sel.title.ifBlank { "(제목 없음)" }))
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.exportToDownloads(
                                context = ctx,
                                onDone = { ok, msg ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (ok) "백업 완료: $msg" else "백업 실패: $msg"
                                        )
                                    }
                                }
                            )
                        }
                    ) { Text("전체백업") }

                    TextButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
                    ) { Text("가져오기") }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(Modifier.fillMaxSize()) {

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        // 검색 변경 시 선택 해제
                        selectedNoteId = null
                        viewModel.setQuery(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    singleLine = true,
                    label = { Text("검색 (제목/내용)") },
                    placeholder = { Text("검색어를 입력하세요") }
                )

                if (notes.isEmpty()) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Text(
                            "메모가 없습니다.\n오른쪽 아래 ‘새 메모’ 버튼을 눌러 작성해 보세요.",
                            modifier = Modifier.align(Alignment.TopCenter),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp) // FAB 공간
                    ) {
                        items(notes, key = { it.id }) { note ->
                            val isSelected = note.id == selectedNoteId
                            val dueLabel = note.dueAt?.let {
                                if (it > now) "⏰ ${fmt.format(Date(it))}" else null
                            }

                            // 카드 배경색으로 선택 표시
                            val containerColor =
                                if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                else MaterialTheme.colorScheme.surface

                            Card(
                                colors = CardDefaults.cardColors(containerColor = containerColor),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .combinedClickable(
                                        onClick = {
                                            selectedNoteId =
                                                if (isSelected) null else note.id
                                        },
                                        onLongClick = { onOpen(note.id) }
                                    )
                            ) {
                                Column(
                                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (note.title.isBlank()) "(제목 없음)" else note.title,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        if (dueLabel != null) {
                                            Text(
                                                text = dueLabel,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = note.content,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 좌하단 공유 FAB 자리 교체
            val canShare = selectedNoteId != null

            FloatingActionButton(
                onClick = {
                    if (canShare) {
                        selectedNote()?.let { shareNote(ctx, it) }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                // 상태별 색상: 비활성(연한 회색/보조) vs 활성(Primary)
                containerColor = if (canShare)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (canShare)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "공유"
                )
            }

            // ✅ 우하단: 새 메모 FAB
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = "추가") },
                text = { Text("새 메모") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }
}

private fun shareNote(context: android.content.Context, note: Note) {
    val title = if (note.title.isBlank()) "(제목 없음)" else note.title
    val body = buildString {
        appendLine(title)
        if (note.content.isNotBlank()) {
            appendLine()
            append(note.content)
        }
        note.dueAt?.let {
            appendLine()
            appendLine("일정: ${SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(it))}")
        }
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, body)
    }
    context.startActivity(Intent.createChooser(intent, "메모 공유"))
}
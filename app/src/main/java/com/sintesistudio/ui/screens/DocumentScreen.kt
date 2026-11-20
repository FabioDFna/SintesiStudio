package com.sintesistudio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sintesistudio.ui.TtsController
import com.sintesistudio.ui.viewmodel.DocumentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScreen(
    viewModel: DocumentViewModel,
    onBack: () -> Unit
) {
    val doc by viewModel.document.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var question by remember { mutableStateOf("") }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val tts = remember { TtsController(context) }
    val chatList = rememberLazyListState()

    LaunchedEffect(error) {
        error?.let { snackbar.showSnackbar(it) }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) chatList.animateScrollToItem(messages.lastIndex)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(doc?.title ?: "Documento") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            doc?.parseWarnings?.takeIf { it.isNotBlank() }?.let { w ->
                Text(
                    "Avvisi parsing: $w",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Sintesi") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Podcast TTS") })
                Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Q&A") })
            }
            if (loading) CircularProgressIndicator(Modifier.padding(16.dp))

            when (tab) {
                0 -> SummaryTab(
                    summary = doc?.summary,
                    onGenerate = { viewModel.generateSummary() }
                )
                1 -> AudioTab(
                    script = doc?.audioScript,
                    onGenerate = { viewModel.generateAudio() },
                    onPlay = { doc?.audioScript?.let { tts.speak(it) } },
                    onStop = { tts.stop() }
                )
                2 -> QaTab(
                    messages = messages,
                    question = question,
                    onQuestionChange = { question = it },
                    onAsk = {
                        viewModel.ask(question)
                        question = ""
                    },
                    listState = chatList
                )
            }
        }
    }
}

@Composable
private fun SummaryTab(summary: String?, onGenerate: () -> Unit) {
    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scroll)
    ) {
        if (summary == null) {
            Text("Genera un riassunto ampio per capitoli/macro-aree.")
            Button(onClick = onGenerate, Modifier.padding(top = 12.dp)) {
                Text("Genera sintesi")
            }
        } else {
            Text(summary)
            Button(onClick = onGenerate, Modifier.padding(top = 8.dp)) {
                Text("Rigenera sintesi")
            }
        }
    }
}

@Composable
private fun AudioTab(
    script: String?,
    onGenerate: () -> Unit,
    onPlay: () -> Unit,
    onStop: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Pronta per la lettura vocale — ascolta mentre fai altro.",
            style = MaterialTheme.typography.labelLarge
        )
        if (script == null) {
            Button(onClick = onGenerate, Modifier.padding(top = 12.dp)) {
                Text("Genera script podcast")
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                IconButton(onClick = onPlay) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Ascolta")
                }
                IconButton(onClick = onStop) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop")
                }
                Button(onClick = onGenerate) { Text("Rigenera") }
            }
            Text(
                script,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        }
    }
}

@Composable
private fun QaTab(
    messages: List<com.sintesistudio.data.entity.ChatMessageEntity>,
    question: String,
    onQuestionChange: (String) -> Unit,
    onAsk: () -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Domande basate sul testo completo in memoria.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                val label = if (msg.role == "user") "Tu" else "Assistente"
                Text("$label: ${msg.content}", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = question,
                onValueChange = onQuestionChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Chiedi su un capitolo...") }
            )
            Button(onClick = onAsk, enabled = question.isNotBlank()) {
                Text("Invia")
            }
        }
    }
}

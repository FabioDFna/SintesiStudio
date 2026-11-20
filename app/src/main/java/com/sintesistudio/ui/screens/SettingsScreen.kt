package com.sintesistudio.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.sintesistudio.ai.GeminiService
import com.sintesistudio.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val savedKey by viewModel.apiKey.collectAsState()
    val savedModel by viewModel.geminiModel.collectAsState()
    var keyInput by remember(savedKey) { mutableStateOf(savedKey.orEmpty()) }
    var selectedModel by remember(savedModel) {
        mutableStateOf(savedModel.ifBlank { GeminiService.DEFAULT_MODEL })
    }

    val modelOptions = listOf(
        GeminiService.DEFAULT_MODEL to "Flash 1.5 (consigliato)",
        "gemini-2.5-flash" to "Flash 2.5",
        "gemini-2.5-flash-lite" to "Flash-Lite 2.5"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Impostazioni") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("API key Google Gemini (aistudio.google.com/apikey)")
            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                label = { Text("Gemini API Key") }
            )

            Text(
                "Modello AI",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                "Evita gemini-2.0-flash: non ha più quota gratuita.",
                style = MaterialTheme.typography.bodySmall
            )
            modelOptions.forEach { (id, label) ->
                FilterChip(
                    selected = selectedModel == id,
                    onClick = { selectedModel = id },
                    label = { Text(label) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Button(
                onClick = {
                    viewModel.saveKey(keyInput)
                    viewModel.saveModel(selectedModel)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salva")
            }
        }
    }
}

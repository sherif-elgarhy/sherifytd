package com.sherifytd.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sherifytd.app.downloader.DownloadViewModel
import com.sherifytd.app.downloader.DownloadViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    url: String,
    fmt: String,
    playlist: Boolean,
    subs: Boolean,
    sponsorblock: Boolean,
    archive: Boolean,
    subLangs: String,
    onBack: () -> Unit,
) {
    val context   = LocalContext.current
    val vm: DownloadViewModel = viewModel(factory = DownloadViewModelFactory(context))
    val lines     by vm.lines.collectAsState()
    val isDone    by vm.isDone.collectAsState()
    val isError   by vm.isError.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        vm.start(url, fmt, playlist, subs, sponsorblock, archive, subLangs)
    }
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isDone) "Done ✓" else if (isError) "Failed ✗" else "Downloading…") },
                navigationIcon = {
                    if (isDone || isError) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                },
                actions = {
                    if (!isDone && !isError) {
                        IconButton(onClick = { vm.cancel(); onBack() }) {
                            Icon(Icons.Default.Cancel, "Cancel")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (!isDone && !isError) LinearProgressIndicator(Modifier.fillMaxWidth())

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(lines) { line ->
                    Text(
                        text  = line,
                        fontFamily = FontFamily.Monospace,
                        fontSize   = 12.sp,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (isDone || isError) {
                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) { Text(if (isDone) "Back to Home" else "Try Again") }
            }
        }
    }
}

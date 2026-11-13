package com.sherifytd.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sherifytd.app.FORMATS

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    sharedUrl: String? = null,
    onStartDownload: (url: String, fmtIndex: Int, playlist: Boolean, subs: Boolean,
                      sponsorblock: Boolean, archive: Boolean, subLangs: String) -> Unit,
) {
    var url             by remember { mutableStateOf(sharedUrl ?: "") }
    var fmtIndex        by remember { mutableIntStateOf(0) }
    var playlist        by remember { mutableStateOf(false) }
    var subs            by remember { mutableStateOf(false) }
    var sponsorblock    by remember { mutableStateOf(false) }
    var archive         by remember { mutableStateOf(false) }
    var subLangs        by remember { mutableStateOf("en,ar") }
    var fmtMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SherifYTD") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Paste URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            ExposedDropdownMenuBox(
                expanded = fmtMenuExpanded,
                onExpandedChange = { fmtMenuExpanded = it },
            ) {
                OutlinedTextField(
                    value = FORMATS[fmtIndex].label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Format") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(fmtMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = fmtMenuExpanded,
                    onDismissRequest = { fmtMenuExpanded = false },
                ) {
                    FORMATS.forEachIndexed { index, fmt ->
                        DropdownMenuItem(
                            text = { Text(fmt.label) },
                            onClick = { fmtIndex = index; fmtMenuExpanded = false },
                        )
                    }
                }
            }

            Card {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Options", style = MaterialTheme.typography.labelLarge,
                         modifier = Modifier.padding(bottom = 4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = playlist, onCheckedChange = { playlist = it })
                        Text("Full playlist")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = subs, onCheckedChange = { subs = it })
                        Text("Embed subtitles")
                    }
                    if (subs) {
                        OutlinedTextField(
                            value = subLangs,
                            onValueChange = { subLangs = it },
                            label = { Text("Languages (e.g. en,ar)") },
                            modifier = Modifier.fillMaxWidth().padding(start = 40.dp),
                            singleLine = true,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = sponsorblock, onCheckedChange = { sponsorblock = it })
                        Text("SponsorBlock")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = archive, onCheckedChange = { archive = it })
                        Text("Skip already downloaded")
                    }
                }
            }

            Button(
                onClick = {
                    if (url.isNotBlank())
                        onStartDownload(url.trim(), fmtIndex, playlist, subs, sponsorblock, archive, subLangs)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = url.isNotBlank(),
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Download")
            }
        }
    }
}

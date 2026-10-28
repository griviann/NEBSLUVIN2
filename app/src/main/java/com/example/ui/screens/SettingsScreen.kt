package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KisumuEmerald
import com.example.ui.theme.KisumuForestGreen
import com.example.ui.theme.StatusActionFailed
import com.example.ui.theme.StatusConnectionError
import com.example.ui.theme.StatusReady
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val healthReport by viewModel.healthReport.collectAsState()
    val fbPageInfo by viewModel.facebookPageInfo.collectAsState()
    val pillars by viewModel.contentPillars.collectAsState()
    val moderationRules by viewModel.moderationRules.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()

    var showAddPillarDialog by remember { mutableStateOf(false) }
    var newPillarTitle by remember { mutableStateOf("") }
    var newPillarDesc by remember { mutableStateOf("") }

    var newBlockedWord by remember { mutableStateOf("") }

    // FB Form
    var fbPageId by remember { mutableStateOf("") }
    var fbToken by remember { mutableStateOf("") }
    var fbVersion by remember { mutableStateOf("v20.0") }
    var isTokenVisible by remember { mutableStateOf(false) }

    // Groq Form
    var groqKey by remember { mutableStateOf("") }
    var groqModel by remember { mutableStateOf("llama-3.3-70b-versatile") }
    var isGroqKeyVisible by remember { mutableStateOf(false) }

    // Audit Log Filter
    var selectedLogFilter by remember { mutableStateOf("ALL") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Health Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hali ya Mfumo (Health Check)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KisumuForestGreen
                        )
                        IconButton(onClick = { viewModel.refreshHealthCheck() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = KisumuForestGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    healthReport?.let { h ->
                        HealthRow(
                            label = "Facebook Graph API",
                            isOk = h.facebookConnected,
                            detail = if (h.facebookConnected) "${h.facebookPageName} (Connected)" else h.facebookDetails
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        HealthRow(
                            label = "Groq AI Engine",
                            isOk = h.aiConnected,
                            detail = if (h.aiConnected) "${h.aiModel} (Configured)" else h.aiDetails
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        HealthRow(
                            label = "Hifadhi ya Data (Room)",
                            isOk = h.storageHealthy,
                            detail = "SQLite Database imara, rasimu ${h.pendingDraftsCount} zinasubiri"
                        )
                    }
                }
            }
        }

        // 2. Facebook Connection Settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = KisumuForestGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mipangilio ya Facebook Page",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Weka FB_PAGE_ID na FB_PAGE_ACCESS_TOKEN. Hatua hizi hutumia Meta Graph API rasmi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = fbPageId,
                        onValueChange = { fbPageId = it },
                        label = { Text("FB Page ID") },
                        placeholder = { Text("Mfano: 104829104812345") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("fb_page_id_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = fbToken,
                        onValueChange = { fbToken = it },
                        label = { Text("Page Access Token") },
                        placeholder = { Text("EAABw...") },
                        visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                                Icon(
                                    imageVector = if (isTokenVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("fb_token_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = fbVersion,
                        onValueChange = { fbVersion = it },
                        label = { Text("Graph API Version") },
                        placeholder = { Text("v20.0") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.saveFacebookCredentials(fbPageId, fbToken, fbVersion)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KisumuForestGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_fb_credentials_button")
                    ) {
                        Text("Hifadhi na Jaribu Uunganisho (Save & Test)")
                    }

                    // Permissions Checklist
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Meta Graph API Permissions zinazohitajika:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    listOf(
                        "pages_show_list" to "Onyesha kurasa zilizoidhinishwa",
                        "pages_read_engagement" to "Soma takwimu na maoni ya ukurasa",
                        "pages_manage_posts" to "Chapisha machapisho ya maandishi",
                        "pages_read_user_content" to "Soma ujumbe na maoni ya watumiaji",
                        "pages_manage_engagement" to "Jibu maoni ya watumiaji"
                    ).forEach { (perm, desc) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KisumuEmerald, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "$perm ($desc)", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 3. Groq AI Engine Settings
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.SmartToy, contentDescription = null, tint = KisumuForestGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Mipangilio ya Groq AI Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = groqKey,
                        onValueChange = { groqKey = it },
                        label = { Text("GROQ API KEY") },
                        placeholder = { Text("gsk_...") },
                        visualTransformation = if (isGroqKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isGroqKeyVisible = !isGroqKeyVisible }) {
                                Icon(
                                    imageVector = if (isGroqKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("groq_key_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = groqModel,
                        onValueChange = { groqModel = it },
                        label = { Text("AI Model") },
                        placeholder = { Text("llama-3.3-70b-versatile") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("groq_model_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.saveGroqCredentials(groqKey, groqModel)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KisumuForestGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_groq_credentials_button")
                    ) {
                        Text("Hifadhi Mipangilio ya AI")
                    }
                }
            }
        }

        // 4. Content Pillars Management
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Nguzo za Maudhui (Content Pillars)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KisumuForestGreen
                        )
                        IconButton(onClick = { showAddPillarDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Pillar", tint = KisumuForestGreen)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    pillars.forEach { pillar ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = pillar.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(text = pillar.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 11.sp)
                            }
                            Switch(
                                checked = pillar.enabled,
                                onCheckedChange = { viewModel.toggleContentPillar(pillar) },
                                colors = SwitchDefaults.colors(checkedThumbColor = KisumuForestGreen)
                            )
                        }
                    }
                }
            }
        }

        // 5. Moderation & Blocked Words List
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = KisumuForestGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Udhibiti wa Maudhui (Moderation Rules)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Maneno yaliyopigwa marufuku (Prohibited words list):",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        moderationRules.forEach { rule ->
                            InputChip(
                                selected = false,
                                onClick = {},
                                label = { Text(rule.blockedWord) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { viewModel.deleteBlockedWord(rule.id) },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Delete", modifier = Modifier.size(12.dp))
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newBlockedWord,
                            onValueChange = { newBlockedWord = it },
                            placeholder = { Text("Ongeza neno jipya...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newBlockedWord.isNotBlank()) {
                                    viewModel.addBlockedWord(newBlockedWord)
                                    newBlockedWord = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KisumuForestGreen)
                        ) {
                            Text("Ongeza")
                        }
                    }
                }
            }
        }

        // 6. Audit Trail & System Logs
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kumbukumbu za Matukio (Audit Logs)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = KisumuForestGreen
                        )
                        IconButton(onClick = { viewModel.clearAuditLogs() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear Logs", tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Event Filter Chips
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("ALL", "POSTED", "DRAFT_CREATED", "MODERATION_BLOCKED", "ERROR", "COMMENT_FETCHED").forEach { f ->
                            FilterChip(
                                selected = selectedLogFilter == f,
                                onClick = { selectedLogFilter = f },
                                label = { Text(f, fontSize = 10.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val filteredLogs = if (selectedLogFilter == "ALL") auditLogs else auditLogs.filter { it.eventType == selectedLogFilter }
                    val timeFormat = SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault())

                    if (filteredLogs.isEmpty()) {
                        Text("Hakuna matukio ya kategoria hii.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    } else {
                        filteredLogs.take(15).forEach { log ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "[${log.eventType}]",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (log.eventType == "ERROR" || log.eventType == "MODERATION_BLOCKED") StatusActionFailed else KisumuForestGreen
                                        )
                                        Text(
                                            text = timeFormat.format(Date(log.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = log.message, style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                                    if (!log.safeDetails.isNullOrBlank()) {
                                        Text(text = log.safeDetails, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Pillar Dialog
    if (showAddPillarDialog) {
        AlertDialog(
            onDismissRequest = { showAddPillarDialog = false },
            title = { Text("Ongeza Nguzo ya Maudhui (Add Content Pillar)") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPillarTitle,
                        onValueChange = { newPillarTitle = it },
                        label = { Text("Jina la Nguzo") },
                        placeholder = { Text("Mfano: Ajira ya Vijana Kisumu") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPillarDesc,
                        onValueChange = { newPillarDesc = it },
                        label = { Text("Maelezo (Description)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPillarTitle.isNotBlank()) {
                            viewModel.addContentPillar(newPillarTitle, newPillarDesc)
                            newPillarTitle = ""
                            newPillarDesc = ""
                            showAddPillarDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KisumuForestGreen)
                ) {
                    Text("Hifadhi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPillarDialog = false }) {
                    Text("Ghairi")
                }
            }
        )
    }
}

@Composable
fun HealthRow(label: String, isOk: Boolean, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isOk) StatusReady else StatusConnectionError)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PostDraftEntity
import com.example.ui.theme.KisumuEmerald
import com.example.ui.theme.KisumuForestGreen
import com.example.ui.theme.StatusActionFailed
import com.example.ui.theme.StatusAwaitingApproval
import com.example.ui.theme.StatusDraftReady
import com.example.ui.theme.StatusPublished
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val drafts by viewModel.postDrafts.collectAsState()
    val pillars by viewModel.contentPillars.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Awaiting Approval", "All Drafts", "Published", "New Post")

    var editingDraft by remember { mutableStateOf<PostDraftEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PrimaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = KisumuForestGreen
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        val badgeCount = when (index) {
                            0 -> drafts.count { it.status == "AWAITING APPROVAL" }
                            1 -> drafts.size
                            2 -> drafts.count { it.status == "PUBLISHED" }
                            else -> 0
                        }
                        Text(
                            text = if (badgeCount > 0 && index < 3) "$title ($badgeCount)" else title,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    },
                    modifier = Modifier.testTag("tab_$index")
                )
            }
        }

        when (selectedTabIndex) {
            0 -> {
                val awaiting = drafts.filter { it.status == "AWAITING APPROVAL" }
                DraftsListView(
                    drafts = awaiting,
                    emptyMessage = "Hakuna machapisho yanayosubiri idhini kwa sasa.",
                    onApprove = { viewModel.approveDraft(it) },
                    onPublish = { viewModel.publishDraft(it) },
                    onReject = { viewModel.rejectDraft(it) },
                    onEdit = { editingDraft = it },
                    onDelete = { viewModel.deleteDraft(it) }
                )
            }
            1 -> {
                DraftsListView(
                    drafts = drafts,
                    emptyMessage = "Hakuna rasimu zilizohifadhiwa.",
                    onApprove = { viewModel.approveDraft(it) },
                    onPublish = { viewModel.publishDraft(it) },
                    onReject = { viewModel.rejectDraft(it) },
                    onEdit = { editingDraft = it },
                    onDelete = { viewModel.deleteDraft(it) }
                )
            }
            2 -> {
                val published = drafts.filter { it.status == "PUBLISHED" }
                DraftsListView(
                    drafts = published,
                    emptyMessage = "Bado hakuna machapisho yaliyochapishwa kwenye Facebook.",
                    onApprove = { },
                    onPublish = { },
                    onReject = { },
                    onEdit = { },
                    onDelete = { viewModel.deleteDraft(it) }
                )
            }
            3 -> {
                CreatePostSection(
                    pillars = pillars.map { it.title },
                    onCreate = { pillar, content, language ->
                        viewModel.createManualDraft(pillar, content, language)
                        selectedTabIndex = 0
                    }
                )
            }
        }
    }

    // Edit Draft Dialog
    if (editingDraft != null) {
        var editText by remember(editingDraft) { mutableStateOf(editingDraft?.content ?: "") }
        AlertDialog(
            onDismissRequest = { editingDraft = null },
            title = { Text("Rekebisha Chapisho (Edit Draft)") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 6
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Herufi: ${editText.length}/280",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (editText.length > 280) StatusActionFailed else Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        editingDraft?.id?.let { id ->
                            viewModel.editDraft(id, editText)
                        }
                        editingDraft = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KisumuForestGreen)
                ) {
                    Text("Hifadhi (Save)")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingDraft = null }) {
                    Text("Ghairi (Cancel)")
                }
            }
        )
    }
}

@Composable
fun DraftsListView(
    drafts: List<PostDraftEntity>,
    emptyMessage: String,
    onApprove: (Long) -> Unit,
    onPublish: (Long) -> Unit,
    onReject: (Long) -> Unit,
    onEdit: (PostDraftEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    if (drafts.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(drafts, key = { it.id }) { draft ->
                PostDraftCard(
                    draft = draft,
                    onApprove = { onApprove(draft.id) },
                    onPublish = { onPublish(draft.id) },
                    onReject = { onReject(draft.id) },
                    onEdit = { onEdit(draft) },
                    onDelete = { onDelete(draft.id) }
                )
            }
        }
    }
}

@Composable
fun PostDraftCard(
    draft: PostDraftEntity,
    onApprove: () -> Unit,
    onPublish: () -> Unit,
    onReject: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(draft.createdAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(KisumuForestGreen.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = draft.pillarTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = KisumuForestGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${draft.language}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }

                StatusBadge(status = draft.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = draft.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!draft.moderationReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = StatusActionFailed.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = StatusActionFailed,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = draft.moderationReason,
                            style = MaterialTheme.typography.labelSmall,
                            color = StatusActionFailed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${draft.content.length} chars • $formattedTime",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Action Buttons
            if (draft.status != "PUBLISHED") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (draft.status != "AWAITING APPROVAL") {
                        FilledTonalButton(
                            onClick = onApprove,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Idhinisha (Approve)", fontSize = 11.sp)
                        }
                    }
                    Button(
                        onClick = onPublish,
                        colors = ButtonDefaults.buttonColors(containerColor = KisumuForestGreen),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Chapisha FB", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostSection(
    pillars: List<String>,
    onCreate: (pillar: String, content: String, language: String) -> Unit
) {
    var selectedPillar by remember { mutableStateOf(pillars.firstOrNull() ?: "Biashara Ndogo") }
    var expandedDropdown by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("Kiswahili") }
    var postText by remember { mutableStateOf("") }

    val charCount = postText.length
    val sentenceCount = postText.split(Regex("[.!?]+\\s+")).filter { it.isNotBlank() }.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Tunga Chapisho Jipya (Compose Post)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = KisumuForestGreen
            )
            Text(
                text = "Andika chapisho la kijamii kwa wakaazi wa Kisumu. Mfumo utakagua kanuni za lugha na urefu kabla ya kuomba idhini.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            ExposedDropdownMenuBox(
                expanded = expandedDropdown,
                onExpandedChange = { expandedDropdown = !expandedDropdown }
            ) {
                OutlinedTextField(
                    value = selectedPillar,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Nguzo ya Maudhui (Pillar)") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false }
                ) {
                    pillars.forEach { pillar ->
                        DropdownMenuItem(
                            text = { Text(pillar) },
                            onClick = {
                                selectedPillar = pillar
                                expandedDropdown = false
                            }
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedLanguage == "Kiswahili",
                    onClick = { selectedLanguage = "Kiswahili" },
                    label = { Text("Kiswahili") }
                )
                FilterChip(
                    selected = selectedLanguage == "English",
                    onClick = { selectedLanguage = "English" },
                    label = { Text("English") }
                )
            }
        }

        item {
            OutlinedTextField(
                value = postText,
                onValueChange = { postText = it },
                label = { Text("Ujumbe wa Chapisho (Post Message)") },
                placeholder = { Text("Andika hapa... (Max 2 sentences, ~280 herufi)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .testTag("compose_post_input"),
                maxLines = 6
            )

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Herufi: $charCount/280",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (charCount > 280) StatusActionFailed else KisumuForestGreen,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sentensi: $sentenceCount/2",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (sentenceCount > 2) StatusAwaitingApproval else Color.Gray
                )
            }
        }

        item {
            Button(
                onClick = {
                    if (postText.isNotBlank()) {
                        onCreate(selectedPillar, postText, selectedLanguage)
                        postText = ""
                    }
                },
                enabled = postText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = KisumuForestGreen),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("submit_post_button")
            ) {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Kagua na Hifadhi Rasimu (Check & Save Draft)")
            }
        }
    }
}

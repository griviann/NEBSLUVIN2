package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChatMessageEntity
import com.example.ui.theme.KisumuEmerald
import com.example.ui.theme.KisumuForestGreen
import com.example.ui.theme.StatusActionFailed
import com.example.ui.theme.StatusAwaitingApproval
import com.example.ui.theme.StatusConnectionError
import com.example.ui.theme.StatusDraftReady
import com.example.ui.theme.StatusNeedsReview
import com.example.ui.theme.StatusPaused
import com.example.ui.theme.StatusProcessing
import com.example.ui.theme.StatusPublished
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusReplySent
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    viewModel: MainViewModel,
    onNavigateToContent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.chatMessages.collectAsState()
    val isAiProcessing by viewModel.isAiProcessing.collectAsState()
    val healthReport by viewModel.healthReport.collectAsState()
    val listState = rememberLazyListState()

    var inputMessage by remember { mutableStateOf("") }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // System Status Banner
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (healthReport?.facebookConnected == true) StatusReady else StatusConnectionError
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (healthReport?.facebookConnected == true) "Facebook: Imeunganishwa" else "Facebook: Haijaunganishwa",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Kisumu, Kenya",
                        style = MaterialTheme.typography.labelSmall,
                        color = KisumuEmerald,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { viewModel.startNewConversation() },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("new_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Conversation",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Quick Command Suggestions
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val suggestions = listOf(
                "Biashara Ndogo Kisumu" to "Tengeneza chapisho kuhusu biashara ndogo Kisumu",
                "Ziwa Victoria" to "Andaa chapisho kuhusu kulinda Ziwa Victoria na unionyeshe kabla ya kuchapisha",
                "Maoni Mapya" to "Nionyeshe maoni mapya kwenye Ukurasa wa Facebook",
                "Health Check" to "Run a system health check",
                "Sitisha Machapisho" to "Sitisha machapisho ya kiotomatiki",
                "Vitendo 10 vya Mwisho" to "Show me the last ten actions performed by the bot"
            )

            suggestions.forEach { (label, command) ->
                SuggestionChip(
                    onClick = {
                        inputMessage = command
                        viewModel.sendUserInstruction(command)
                        inputMessage = ""
                    },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("chip_$label")
                )
            }
        }

        // Message List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatMessageItem(
                    message = msg,
                    onApproveDraft = { draftId ->
                        viewModel.approveDraft(draftId)
                    },
                    onPublishDraft = { draftId ->
                        viewModel.publishDraft(draftId)
                    },
                    onNavigateToContent = onNavigateToContent
                )
            }

            if (isAiProcessing) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = KisumuForestGreen
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "RealSatisfaction AI inachakata agizo lako...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Chat Input Box
        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 4.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = inputMessage,
                    onValueChange = { inputMessage = it },
                    placeholder = {
                        Text(
                            "Agiza kwa Kiswahili au Kiingereza...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input"),
                    maxLines = 4,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KisumuForestGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputMessage.isNotBlank() && !isAiProcessing) {
                            val textToSend = inputMessage
                            inputMessage = ""
                            viewModel.sendUserInstruction(textToSend)
                        }
                    },
                    enabled = inputMessage.isNotBlank() && !isAiProcessing,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputMessage.isNotBlank() && !isAiProcessing) KisumuForestGreen else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .testTag("send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Tuma Agizo",
                        tint = if (inputMessage.isNotBlank() && !isAiProcessing) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessageEntity,
    onApproveDraft: (Long) -> Unit,
    onPublishDraft: (Long) -> Unit,
    onNavigateToContent: () -> Unit
) {
    val isUser = message.role == "user"
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 2.dp, start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = KisumuForestGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "RealSatisfaction AI",
                    style = MaterialTheme.typography.labelSmall,
                    color = KisumuForestGreen,
                    fontWeight = FontWeight.Bold
                )

                if (message.statusLabel != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    StatusBadge(status = message.statusLabel)
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) KisumuForestGreen else MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = if (isUser) 0.dp else 1.dp,
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = if (isUser) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.End)
                )

                // Contextual action card for post drafts
                if (message.actionType == "DRAFT_POST" && message.actionPayload != null) {
                    val draftId = message.actionPayload.toLongOrNull()
                    if (draftId != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "Hatua ya Haraka / Quick Action:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(
                                        onClick = { onApproveDraft(draftId) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Idhinisha (Approve)", fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = { onPublishDraft(draftId) },
                                        colors = ButtonDefaults.buttonColors(containerColor = KisumuForestGreen),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Chapisha FB", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (bgColor, textColor) = when (status.uppercase(Locale.ROOT)) {
        "READY" -> StatusReady to Color.White
        "PROCESSING" -> StatusProcessing to Color.White
        "DRAFT READY" -> StatusDraftReady to Color.White
        "AWAITING APPROVAL" -> StatusAwaitingApproval to Color.White
        "PUBLISHED" -> StatusPublished to Color.White
        "REPLY SENT" -> StatusReplySent to Color.White
        "PAUSED" -> StatusPaused to Color.White
        "NEEDS REVIEW" -> StatusNeedsReview to Color.White
        "CONNECTION ERROR" -> StatusConnectionError to Color.White
        "ACTION FAILED" -> StatusActionFailed to Color.White
        else -> Color.Gray to Color.White
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = status,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

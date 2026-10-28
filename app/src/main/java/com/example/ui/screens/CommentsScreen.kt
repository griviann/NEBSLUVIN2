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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PageCommentEntity
import com.example.ui.theme.KisumuEmerald
import com.example.ui.theme.KisumuForestGreen
import com.example.ui.theme.KisumuLakeBlue
import com.example.ui.theme.StatusActionFailed
import com.example.ui.theme.StatusAwaitingApproval
import com.example.ui.theme.StatusNeedsReview
import com.example.ui.theme.StatusPublished
import com.example.ui.theme.StatusReady
import com.example.ui.theme.StatusReplySent
import com.example.ui.viewmodel.MainViewModel

@Composable
fun CommentsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val comments by viewModel.pageComments.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header with Refresh & Policy
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Maoni ya Ukurasa wa Facebook",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = KisumuForestGreen
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = KisumuEmerald,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sera: Idhini ya Msimamizi (Human-in-the-Loop)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = { viewModel.fetchComments() },
                    colors = ButtonDefaults.buttonColors(containerColor = KisumuForestGreen),
                    modifier = Modifier.testTag("fetch_comments_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pakua Maoni", fontSize = 12.sp)
                }
            }
        }

        if (comments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Hakuna maoni yaliyopakuliwa bado.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(onClick = { viewModel.fetchComments() }) {
                        Text("Pakua Maoni kutoka Facebook")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(comments, key = { it.id }) { comment ->
                    CommentItemCard(
                        comment = comment,
                        onSaveDraftReply = { reply ->
                            viewModel.updateCommentDraftReply(comment.id, reply)
                        },
                        onSendReply = {
                            viewModel.sendCommentReply(comment.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CommentItemCard(
    comment: PageCommentEntity,
    onSaveDraftReply: (String) -> Unit,
    onSendReply: () -> Unit
) {
    var isEditingReply by remember { mutableStateOf(false) }
    var currentReplyText by remember(comment.draftReply) { mutableStateOf(comment.draftReply ?: "") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Author & Category Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(KisumuForestGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = comment.authorName.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = KisumuForestGreen,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = comment.authorName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Ref: #${comment.postId.takeLast(6)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            fontSize = 10.sp
                        )
                    }
                }

                CategoryBadge(category = comment.category)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Comment text
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "\"${comment.message}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reply section
            if (comment.replyStatus == "REPLY_SENT") {
                Surface(
                    color = StatusReplySent.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusReplySent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Jibu limetumwa kwenye Facebook:",
                                style = MaterialTheme.typography.labelSmall,
                                color = StatusReplySent,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = comment.sentReplyText ?: comment.draftReply ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else {
                // Draft or input reply
                Column {
                    OutlinedTextField(
                        value = currentReplyText,
                        onValueChange = { currentReplyText = it },
                        label = { Text("Jibu Lililoandaliwa (Draft Reply)") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = { onSaveDraftReply(currentReplyText) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Hifadhi Jibu", fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                onSaveDraftReply(currentReplyText)
                                onSendReply()
                            },
                            enabled = currentReplyText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = KisumuForestGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Tuma Facebook", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBadge(category: String) {
    val (bgColor, textColor) = when (category) {
        "Positive feedback" -> StatusReady.copy(alpha = 0.15f) to StatusReady
        "General question" -> KisumuLakeBlue.copy(alpha = 0.15f) to KisumuLakeBlue
        "Complaint" -> StatusActionFailed.copy(alpha = 0.15f) to StatusActionFailed
        "Business inquiry" -> KisumuForestGreen.copy(alpha = 0.15f) to KisumuForestGreen
        "Spam" -> Color.Gray.copy(alpha = 0.15f) to Color.DarkGray
        "Sensitive issue" -> StatusAwaitingApproval.copy(alpha = 0.15f) to StatusAwaitingApproval
        "Needs human attention" -> StatusNeedsReview.copy(alpha = 0.15f) to StatusNeedsReview
        else -> Color.LightGray to Color.Black
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = category,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

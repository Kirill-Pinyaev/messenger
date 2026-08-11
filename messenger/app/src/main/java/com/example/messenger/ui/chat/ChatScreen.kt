package com.example.messenger.ui.chat

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.messenger.data.isUnauthenticatedError
import com.example.messenger.ui.components.GlassCircleButton
import com.example.messenger.ui.components.OnlineBadge
import com.example.messenger.ui.theme.AppBackground
import com.example.messenger.ui.theme.BubbleMe
import com.example.messenger.ui.theme.BubbleMeText
import com.example.messenger.ui.theme.BubbleOther
import com.example.messenger.ui.theme.BubbleOtherText
import com.example.messenger.ui.theme.ChatBackground
import com.example.messenger.ui.theme.InputBackground
import com.example.messenger.ui.theme.Panel
import com.example.messenger.ui.theme.TextMuted
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    conversationId: String,
    peerName: String,
    peerUsername: String,
    isGroup: Boolean,
    onAuthExpired: () -> Unit,
    onBack: () -> Unit
) {
    val vm: ChatViewModel = viewModel()
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var selectedAttachment by remember { mutableStateOf<Uri?>(null) }
    var selectedAttachmentLabel by remember { mutableStateOf("") }
    var imagePreview by remember { mutableStateOf<ChatAttachment?>(null) }
    var pendingSaveImage by remember { mutableStateOf<ChatAttachment?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedAttachment = uri
            selectedAttachmentLabel = uri.lastPathSegment ?: "Вложение"
        }
    }
    val saveImageLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/*")) { uri ->
            val image = pendingSaveImage
            if (uri != null && image?.imageBytes != null) {
                context.contentResolver.openOutputStream(uri)?.use { it.write(image.imageBytes) }
            }
            pendingSaveImage = null
        }

    LaunchedEffect(Unit) { vm.init(conversationId, peerUsername, isGroup) }
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }
    LaunchedEffect(state.sendError) {
        state.sendError?.let {
            snackbar.showSnackbar(it)
            vm.clearSendError()
        }
    }
    LaunchedEffect(state.error, state.sendError) {
        if (isUnauthenticatedError(state.error) || isUnauthenticatedError(state.sendError)) {
            onAuthExpired()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChatBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ChatHeader(
                peerName = peerName,
                isGroup = isGroup,
                onBack = onBack
            )
            Box(modifier = Modifier.weight(1f)) {
                when {
                    state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    state.error != null -> Text(
                        text = "Ошибка: ${state.error}",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(state.messages, key = { it.id }) { msg ->
                            MessageBubble(
                                msg = msg,
                                onPreviewImage = { imagePreview = it }
                            )
                        }
                    }
                }
            }
            ChatComposer(
                value = input,
                onValueChange = { input = it },
                attachmentLabel = selectedAttachmentLabel,
                onPickAttachment = { picker.launch(arrayOf("*/*")) },
                onRemoveAttachment = {
                    selectedAttachment = null
                    selectedAttachmentLabel = ""
                },
                onSend = {
                    val text = input.trim()
                    if (text.isBlank() && selectedAttachment == null) return@ChatComposer
                    val picked = selectedAttachment
                    input = ""
                    selectedAttachment = null
                    selectedAttachmentLabel = ""
                    if (isGroup) vm.sendGroup(conversationId, text, picked) else vm.sendDirect(peerUsername, text, picked)
                }
            )
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
        )
    }

    imagePreview?.let { attachment ->
        ImagePreviewDialog(
            attachment = attachment,
            onDismiss = { imagePreview = null },
            onDownload = {
                pendingSaveImage = attachment
                saveImageLauncher.launch(attachment.filename.ifBlank { "image.png" })
            }
        )
    }
}

@Composable
private fun ChatHeader(
    peerName: String,
    isGroup: Boolean,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBackground)
            .padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassCircleButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarBadge(peerName)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = peerName,
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.size(4.dp))
                    OnlineBadge(
                        text = if (isGroup) "Групповой чат" else "E2EE включено",
                        active = !isGroup
                    )
                }
            }
            Box(modifier = Modifier.size(44.dp))
        }
    }
}

@Composable
private fun AvatarBadge(name: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0xFF5B8DEE)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ChatComposer(
    value: String,
    onValueChange: (String) -> Unit,
    attachmentLabel: String,
    onPickAttachment: () -> Unit,
    onRemoveAttachment: () -> Unit,
    onSend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel)
            .imePadding()
            .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 24.dp)
    ) {
        if (attachmentLabel.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = InputBackground,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = attachmentLabel,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Убрать",
                        color = TextMuted,
                        modifier = Modifier.clickable(onClick = onRemoveAttachment)
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            GlassCircleButton(onClick = onPickAttachment) {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = "Вложить файл",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(InputBackground)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = "Сообщение…",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (value.isBlank() && attachmentLabel.isBlank()) InputBackground
                        else MaterialTheme.colorScheme.primary
                    )
                    .clickable(
                        enabled = value.isNotBlank() || attachmentLabel.isNotBlank(),
                        onClick = onSend
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Отправить",
                    tint = if (value.isBlank() && attachmentLabel.isBlank()) TextMuted else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    msg: ChatMessage,
    onPreviewImage: (ChatAttachment) -> Unit
) {
    val context = LocalContext.current
    val isMine = msg.isMine
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
        ) {
            if (!isMine) {
                Text(
                    text = msg.from,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(start = 6.dp, bottom = 2.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isMine) 16.dp else 4.dp,
                    bottomEnd = if (isMine) 4.dp else 16.dp
                ),
                color = if (isMine) BubbleMe else BubbleOther,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                    if (msg.text.isNotBlank()) {
                        Text(
                            text = msg.text,
                            color = if (isMine) BubbleMeText else BubbleOtherText,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    msg.attachments.forEach { attachment ->
                        AttachmentBlock(
                            attachment = attachment,
                            onPreviewImage = onPreviewImage,
                            onOpen = { path, mimeType ->
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    File(path)
                                )
                                val intent = Intent(Intent.ACTION_VIEW)
                                    .setDataAndType(uri, mimeType)
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                context.startActivity(intent)
                            }
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = formatTime(msg.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMine) BubbleMeText.copy(alpha = 0.72f) else TextMuted,
                            fontSize = 10.sp
                        )
                        if (msg.encrypted) {
                            E2eeBadge(error = msg.decryptionError, isMine = isMine)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentBlock(
    attachment: ChatAttachment,
    onPreviewImage: (ChatAttachment) -> Unit,
    onOpen: (String, String) -> Unit
) {
    if (attachment.decryptionError) {
        Text(
            text = "[Не удалось расшифровать вложение]",
            color = Color(0xFFFF7F7F),
            modifier = Modifier.padding(top = 6.dp)
        )
        return
    }
    if (attachment.kind == "image" && attachment.imageBytes != null) {
        val bitmap = remember(attachment.id) {
            BitmapFactory.decodeByteArray(attachment.imageBytes, 0, attachment.imageBytes.size)
                ?.asImageBitmap()
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = attachment.filename,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .widthIn(max = 220.dp)
                    .clickable { onPreviewImage(attachment) }
            )
            return
        }
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = InputBackground,
        modifier = Modifier
            .padding(top = 6.dp)
            .clickable(enabled = attachment.localPath != null) {
                attachment.localPath?.let { onOpen(it, attachment.mimeType) }
            }
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = if (attachment.kind == "video") "Видео" else "Файл",
                color = TextMuted,
                style = MaterialTheme.typography.labelSmall
            )
            Text(text = attachment.filename, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ImagePreviewDialog(
    attachment: ChatAttachment,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    val bitmap = remember(attachment.id) {
        attachment.imageBytes?.let { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Panel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = attachment.filename,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = attachment.filename,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .fillMaxWidth()
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Закрыть")
                    }
                    Button(onClick = onDownload) {
                        Text("Скачать")
                    }
                }
            }
        }
    }
}

@Composable
private fun E2eeBadge(error: Boolean, isMine: Boolean) {
    val bg = when {
        error && isMine -> Color(0x33FF5B5B)
        error -> Color(0x1FFF5B5B)
        isMine -> Color.White.copy(alpha = 0.18f)
        else -> Color(0x1F3BE8A0)
    }
    val textColor = when {
        error && isMine -> Color(0xFFFFC2C2)
        error -> Color(0xFFFF7F7F)
        isMine -> Color.White.copy(alpha = 0.88f)
        else -> Color(0xFF3BE8A0)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(10.dp)
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = if (error) "ошибка E2EE" else "E2EE",
                color = textColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

private fun formatTime(epochSeconds: Long): String = timeFmt.format(Date(epochSeconds * 1000))

package com.example.messenger.ui.conversations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.messenger.data.isUnauthenticatedError
import com.example.messenger.ui.components.GlassCircleButton
import com.example.messenger.ui.components.SearchField
import com.example.messenger.ui.components.SectionTitle
import com.example.messenger.ui.theme.AppBackground
import com.example.messenger.ui.theme.Divider
import com.example.messenger.ui.theme.Panel
import com.example.messenger.ui.theme.TextMuted

@Composable
fun ConversationListScreen(
    onOpenChat: (convId: String, peerName: String, isGroup: Boolean, peerUsername: String) -> Unit,
    onAuthExpired: () -> Unit,
    onLogout: () -> Unit
) {
    val vm: ConversationListViewModel = viewModel()
    val state by vm.state.collectAsState()
    val myUsername by vm.myUsername.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val filteredItems = remember(state.items, searchQuery) {
        filterConversationItems(state.items, searchQuery)
    }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    androidx.compose.runtime.LaunchedEffect(state.error) {
        if (isUnauthenticatedError(state.error)) {
            onAuthExpired()
        }
    }
    androidx.compose.runtime.LaunchedEffect(searchQuery) {
        vm.search(searchQuery)
    }
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        when {
            state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            state.error != null -> Text(
                "Ошибка: ${state.error}",
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.error
            )
            else -> Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassCircleButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Выйти", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(Modifier.size(14.dp))
                SectionTitle("Чаты")
                Spacer(Modifier.size(14.dp))
                SearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Поиск пользователей",
                    leading = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                Spacer(Modifier.size(16.dp))
                if (shouldSearchUsers(searchQuery)) {
                    SearchResultsList(
                        loading = state.searchLoading,
                        items = state.searchResults,
                        onOpenChat = { user ->
                            val convId = directConversationId(myUsername, user.username)
                            onOpenChat(convId, user.displayName, false, user.username)
                        }
                    )
                } else if (filteredItems.isEmpty()) {
                    EmptyState(searchQuery.isNotBlank())
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Panel,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        LazyColumn {
                            items(filteredItems, key = { it.conversationId }) { item ->
                                ConversationRow(
                                    item = item,
                                    onClick = {
                                        val peerUsername = if (item.isGroup) item.displayName
                                        else item.peerUsername.ifEmpty {
                                            item.members.firstOrNull { it != myUsername } ?: item.displayName
                                        }
                                        onOpenChat(item.conversationId, item.displayName, item.isGroup, peerUsername)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultsList(
    loading: Boolean,
    items: List<UserSearchItem>,
    onOpenChat: (UserSearchItem) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Panel,
        shape = RoundedCornerShape(24.dp)
    ) {
        when {
            loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            items.isEmpty() -> EmptyState(isFiltered = true)
            else -> LazyColumn {
                items(items, key = { it.username }) { item ->
                    SearchUserRow(
                        item = item,
                        onClick = { onOpenChat(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(isFiltered: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            if (isFiltered) "Ничего не найдено" else "Нет диалогов",
            color = TextMuted,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ConversationRow(item: ConvItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarCircle(name = item.displayName)
        Spacer(Modifier.width(12.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(end = if (item.isGroup) 28.dp else 0.dp)) {
                Text(
                    item.displayName,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(Modifier.size(2.dp))
                Text(
                    if (item.isGroup) "${item.members.size} участников" else "@${item.peerUsername}",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
            if (item.isGroup) {
                Icon(
                    Icons.Default.Group,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.CenterEnd)
                )
            }
        }
    }
    HorizontalDivider(
        color = Divider,
        modifier = Modifier.padding(start = 76.dp)
    )
}

@Composable
private fun SearchUserRow(item: UserSearchItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarCircle(name = item.displayName)
        Spacer(Modifier.width(12.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(
                    item.displayName,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(Modifier.size(2.dp))
                Text(
                    "@${item.username}",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }
        }
    }
    HorizontalDivider(
        color = Divider,
        modifier = Modifier.padding(start = 76.dp)
    )
}

@Composable
private fun AvatarCircle(name: String) {
    val initial = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val color = avatarColor(name)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initial,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

private fun avatarColor(name: String): Color {
    val colors = listOf(
        Color(0xFF7C6FFF), Color(0xFF5C6BC0), Color(0xFF26A69A),
        Color(0xFF66BB6A), Color(0xFFEF5350), Color(0xFFEC407A),
        Color(0xFFAB47BC), Color(0xFF42A5F5)
    )
    return colors[(name.hashCode() and 0x7FFFFFFF) % colors.size]
}

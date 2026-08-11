package com.example.messenger.ui.conversations

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.messenger.App
import com.example.messenger.data.MessengerRepository
import com.example.messenger.proto.Conversation
import com.example.messenger.proto.ConversationKind
import com.example.messenger.proto.Profile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ConvItem(
    val conversationId: String,
    val displayName: String,
    val peerUsername: String, // actual username for direct chats, empty for groups
    val isGroup: Boolean,
    val members: List<String>
)

data class ConvListState(
    val loading: Boolean = true,
    val items: List<ConvItem> = emptyList(),
    val searchResults: List<UserSearchItem> = emptyList(),
    val searchLoading: Boolean = false,
    val error: String? = null
)

data class UserSearchItem(
    val username: String,
    val displayName: String
)

class ConversationListViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as App
    private var repo: MessengerRepository? = null
    private var searchJob: Job? = null

    private val _state = MutableStateFlow(ConvListState())
    val state: StateFlow<ConvListState> = _state

    private val _myUsername = MutableStateFlow("")
    val myUsername: StateFlow<String> = _myUsername

        init {
            viewModelScope.launch {
            val session = app.tokenStore.load() ?: return@launch
            _myUsername.value = session.username
            repo = MessengerRepository(app.grpc, session.token)
            app.eventService.start(session.token)
            load()
            collectEvents()
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            try {
                val convs = repo!!.listConversations()
                _state.value = _state.value.copy(loading = false, items = convs.map { it.toItem() })
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun refresh() = load()

    fun search(query: String) {
        if (!shouldSearchUsers(query)) {
            searchJob?.cancel()
            _state.value = _state.value.copy(searchResults = emptyList(), searchLoading = false, error = null)
            return
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.value = _state.value.copy(searchLoading = true, error = null)
            try {
                val myUsername = _myUsername.value
                val users = repo!!.searchUsers(query)
                    .filter { it.username != myUsername }
                    .map { it.toSearchItem() }
                _state.value = _state.value.copy(searchResults = users, searchLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(searchResults = emptyList(), searchLoading = false, error = e.message)
            }
        }
    }

    private fun collectEvents() {
        viewModelScope.launch {
            app.eventService.events.collect { event ->
                when {
                    event.hasMessage() -> {
                        val conversationId = event.message.conversationId
                        val existing = _state.value.items.map { it.conversationId }
                        if (shouldReloadForIncomingMessage(existing, conversationId)) {
                            load()
                        }
                    }
                    event.hasConversation() -> {
                        val existing = _state.value.items.map { it.conversationId }
                        if (event.conversation.conversationId !in existing) {
                            _state.value = _state.value.copy(
                                items = _state.value.items + event.conversation.toItem()
                            )
                        }
                    }
                    event.hasConversationRemoved() -> {
                        val id = event.conversationRemoved.conversationId
                        _state.value = _state.value.copy(
                            items = _state.value.items.filter { it.conversationId != id }
                        )
                    }
                    else -> {}
                }
            }
        }
    }

    private fun Conversation.toItem(): ConvItem {
        val isGroup = kind == ConversationKind.CONVERSATION_KIND_GROUP
        return ConvItem(
            conversationId = conversationId,
            displayName = if (isGroup) title.ifEmpty { "Группа" }
                          else peerProfile?.let { "${it.firstName} ${it.lastName}".trim().ifEmpty { peerUsername } } ?: peerUsername,
            peerUsername = if (isGroup) "" else peerUsername,
            isGroup = isGroup,
            members = memberUsernamesList
        )
    }

    private fun Profile.toSearchItem(): UserSearchItem =
        UserSearchItem(
            username = username,
            displayName = "$firstName $lastName".trim().ifEmpty { username }
        )
}

package com.example.messenger.ui.chat

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.messenger.App
import com.example.messenger.crypto.ArchiveE2EE
import com.example.messenger.crypto.E2EE
import com.example.messenger.crypto.IdentityState
import com.example.messenger.data.MessengerRepository
import com.example.messenger.proto.Attachment
import com.example.messenger.proto.AttachmentDirectEnvelope
import com.example.messenger.proto.AttachmentKind
import com.example.messenger.proto.AttachmentPreview
import com.example.messenger.proto.ConversationKeyEnvelope
import com.example.messenger.proto.DirectMessageEnvelope
import com.example.messenger.proto.HistoryArchiveRecordType
import com.example.messenger.proto.historyArchiveRecord
import com.example.messenger.proto.Message
import com.google.protobuf.ByteString
import com.google.protobuf.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.util.UUID

data class ChatAttachment(
    val id: String,
    val kind: String,
    val filename: String,
    val mimeType: String,
    val sizeBytes: Long,
    val imageBytes: ByteArray? = null,
    val localPath: String? = null,
    val decryptionError: Boolean = false
)

data class ChatMessage(
    val id: Long,
    val conversationId: String,
    val from: String,
    val text: String,
    val createdAt: Long,
    val encrypted: Boolean,
    val decryptionError: Boolean,
    val isMine: Boolean,
    val attachments: List<ChatAttachment> = emptyList()
)

data class ChatUiState(
    val loading: Boolean = true,
    val messages: List<ChatMessage> = emptyList(),
    val error: String? = null,
    val sendError: String? = null
)

private data class PreparedAttachment(
    val proto: Attachment,
    val descriptorJson: String,
    val mediaId: String,
    val filename: String,
    val mimeType: String,
    val kindName: String,
)

private data class ArchivedConversationState(
    val messages: List<ChatMessage>,
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as App
    private var repo: MessengerRepository? = null
    private var myUsername = ""
    private var myDeviceId = ""
    private var token = ""
    private var activeConversationId = ""
    private var activeIsGroup = false

    private val groupKeys = mutableMapOf<String, ByteArray>()
    private val senderPubCache = mutableMapOf<String, ByteArray>()

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state

    fun init(conversationId: String, peerUsername: String, isGroup: Boolean) {
        viewModelScope.launch {
            val session = app.tokenStore.load() ?: return@launch
            token = session.token
            myUsername = session.username
            myDeviceId = session.deviceId
            activeConversationId = conversationId
            activeIsGroup = isGroup
            repo = MessengerRepository(app.grpc, session.token)
            loadMessages(conversationId, if (isGroup) null else peerUsername)
            collectEvents(conversationId)
        }
    }

    private suspend fun loadMessages(conversationId: String, withUsername: String?) {
        _state.value = ChatUiState(loading = true)
        try {
            val identity = app.identityStore.load()
            val archived = loadArchivedConversation(conversationId)
            val raw = repo!!.getMessages(conversationId = conversationId, withUsername = withUsername)
            val decoded = raw.map { decodeMessage(it, identity) }
            _state.value = ChatUiState(loading = false, messages = mergeArchivedMessages(decoded, archived.messages))
        } catch (e: Exception) {
            _state.value = ChatUiState(loading = false, error = e.message)
        }
    }

    private fun collectEvents(conversationId: String) {
        viewModelScope.launch {
            app.eventService.events.collect { event ->
                if (event.hasMessage()) {
                    val msg = event.message
                    val relevant = isMessageRelevantForChat(conversationId, msg)
                    if (!relevant) return@collect
                    val identity = app.identityStore.load()
                    val decoded = decodeMessage(msg, identity)
                    val current = _state.value.messages
                    if (current.none { it.id == decoded.id }) {
                        _state.value = _state.value.copy(messages = current + decoded)
                    }
                }
            }
        }
    }

    private suspend fun loadArchivedConversation(conversationId: String): ArchivedConversationState {
        val archiveIdentity = app.archiveStore.load() ?: return ArchivedConversationState(emptyList())
        val archivedByMessageId = linkedMapOf<Long, ChatMessage>()
        var afterSequence = 0L
        while (true) {
            val page = repo!!.listHistoryArchiveRecords(afterSequence = afterSequence, limit = 500)
            if (page.isEmpty()) {
                break
            }
            for (record in page) {
                afterSequence = maxOf(afterSequence, record.sequence)
                if (record.conversationId != conversationId) {
                    continue
                }
                val decrypted = runCatching {
                    ArchiveE2EE.decryptPayload(
                        encrypted = com.example.messenger.crypto.ArchiveEncrypted(
                            ciphertext = record.ciphertext.toByteArray(),
                            nonce = record.nonce.toByteArray(),
                            ephemeralPublicKey = record.ephemeralPublicKey.toByteArray(),
                        ),
                        identity = archiveIdentity,
                    )
                }.getOrNull() ?: continue
                val payloadText = String(decrypted, Charsets.UTF_8)
                val payload = runCatching { JSONObject(payloadText) }.getOrNull() ?: continue
                when (payload.optString("kind")) {
                    "group_key" -> {
                        val version = payload.optInt("version", record.groupKeyVersion)
                        val bytes = jsonArrayToByteArray(payload.optJSONArray("groupKeyBytes"))
                        if (version > 0 && bytes.isNotEmpty()) {
                            groupKeys["$conversationId:$version"] = bytes
                        }
                    }
                    "message" -> {
                        val messageId = record.messageId
                        if (messageId <= 0) continue
                        archivedByMessageId[messageId] = ChatMessage(
                            id = messageId,
                            conversationId = conversationId,
                            from = payload.optString("from", record.sender),
                            text = payload.optString("text"),
                            createdAt = parseArchiveCreatedAt(payload, record),
                            encrypted = true,
                            decryptionError = false,
                            isMine = payload.optString("from", record.sender) == myUsername,
                            attachments = archivedByMessageId[messageId]?.attachments ?: emptyList(),
                        )
                    }
                    "attachment" -> {
                        val messageId = record.messageId
                        if (messageId <= 0) continue
                        val attachment = try {
                            materializeArchivedAttachment(record, payload)
                        } catch (e: Exception) {
                            Log.w("Archive", "failed to materialize archived attachment msg=${record.messageId} att=${record.attachmentId}: ${e.message}")
                            continue
                        }
                        val existing = archivedByMessageId[messageId]
                        archivedByMessageId[messageId] = ChatMessage(
                            id = messageId,
                            conversationId = conversationId,
                            from = payload.optString("from", existing?.from ?: record.sender),
                            text = payload.optString("text", existing?.text ?: ""),
                            createdAt = parseArchiveCreatedAt(payload, record).takeIf { it > 0 } ?: (existing?.createdAt ?: 0L),
                            encrypted = true,
                            decryptionError = false,
                            isMine = payload.optString("from", existing?.from ?: record.sender) == myUsername,
                            attachments = (existing?.attachments ?: emptyList()).filterNot { it.id == attachment.id } + attachment,
                        )
                    }
                }
            }
            if (page.size < 500) {
                break
            }
        }
        return ArchivedConversationState(archivedByMessageId.values.sortedBy { it.id })
    }

    private suspend fun decodeMessage(msg: Message, identity: IdentityState?): ChatMessage {
        val isMine = msg.from == myUsername
        val decryptAsSender = identity != null && shouldDecryptDirectAsSender(
            messageFrom = msg.from,
            username = myUsername,
            senderDeviceId = msg.senderDeviceId,
            currentDeviceId = myDeviceId,
            recipientSignedPrekeyId = msg.recipientSignedPrekeyId,
            recipientOneTimePrekeyId = msg.recipientOneTimePrekeyId,
            currentSignedPrekeyId = identity.signedPrekeyId,
            currentOneTimePrekeyIds = identity.oneTimePrekeys.map { it.keyId }.toSet(),
        )
        Log.e(
            "MSGDBG",
            "msgId=${msg.messageId} from=${msg.from} to=${msg.to} conv=${msg.conversationId} " +
                "senderDevice=${msg.senderDeviceId} encrypted=${msg.encrypted} " +
                "cipherLen=${msg.ciphertext.size()} nonceLen=${msg.nonce.size()} " +
                "spkId=${msg.recipientSignedPrekeyId} otpId=${msg.recipientOneTimePrekeyId} " +
                "attachments=${msg.attachmentsCount} decryptAsSender=$decryptAsSender activeIsGroup=$activeIsGroup"
        )
        val attachments = decodeAttachments(msg, identity, isMine)
        if (!msg.encrypted || identity == null) {
            return ChatMessage(
                id = msg.messageId,
                conversationId = msg.conversationId,
                from = msg.from,
                text = msg.text,
                createdAt = msg.createdAt?.seconds ?: 0L,
                encrypted = false,
                decryptionError = false,
                isMine = isMine,
                attachments = attachments
            )
        }

        val text = try {
            if (msg.ciphertext.isEmpty) ""
            else if (activeIsGroup) decryptGroupMsg(msg, identity)
            else decryptDirectMsg(msg, identity, decryptAsSender)
        } catch (e: Exception) {
            Log.e("E2EE", "decrypt failed msg=${msg.messageId} from=${msg.from} isMine=$isMine" +
                " otpId=${msg.recipientOneTimePrekeyId} spkPubLen=${msg.recipientSignedPrekeyPublic.size()}" +
                " err=${e.javaClass.simpleName}: ${e.message}", e)
            null
        }

        return ChatMessage(
            id = msg.messageId,
            conversationId = msg.conversationId,
            from = msg.from,
            text = text ?: if (msg.ciphertext.isEmpty) "" else "[Не удалось расшифровать]",
            createdAt = msg.createdAt?.seconds ?: 0L,
            encrypted = true,
            decryptionError = text == null && !msg.ciphertext.isEmpty,
            isMine = isMine,
            attachments = attachments
        )
    }

    private suspend fun decodeAttachments(msg: Message, identity: IdentityState?, isMine: Boolean): List<ChatAttachment> {
        if (identity == null) {
            return emptyList()
        }
        val decryptAsSender = shouldDecryptDirectAsSender(
            messageFrom = msg.from,
            username = myUsername,
            senderDeviceId = msg.senderDeviceId,
            currentDeviceId = myDeviceId,
            recipientSignedPrekeyId = msg.recipientSignedPrekeyId,
            recipientOneTimePrekeyId = msg.recipientOneTimePrekeyId,
            currentSignedPrekeyId = identity.signedPrekeyId,
            currentOneTimePrekeyIds = identity.oneTimePrekeys.map { it.keyId }.toSet(),
        )
        return msg.attachmentsList.map { item ->
            Log.e(
                "ATTDBG",
                "msgId=${msg.messageId} mediaId=${item.mediaId} attachmentId=${item.attachmentId} " +
                    "descLen=${item.encryptedDescriptor.size()} nonceLen=${item.descriptorNonce.size()} " +
                    "mime=${item.mimeType} file=${item.filename} decryptAsSender=$decryptAsSender"
            )
            try {
                val descriptorJson = when {
                    activeIsGroup -> {
                        val key = "${msg.conversationId}:${msg.conversationKeyVersion}"
                        val groupKey = groupKeys[key] ?: fetchAndDecryptGroupKey(
                            msg.conversationId, msg.conversationKeyVersion, identity
                        ).also { groupKeys[key] = it }
                        E2EE.decryptGroupMessage(item.encryptedDescriptor.toByteArray(), item.descriptorNonce.toByteArray(), groupKey)
                    }
                    decryptAsSender -> decryptOwnDescriptor(msg, item, identity)
                    else -> decryptIncomingDescriptor(msg, item, identity)
                }
                val media = repo!!.getMedia(item.mediaId)
                val plaintext = E2EE.decryptMedia(media.ciphertext.toByteArray(), media.nonce.toByteArray(), descriptorJson)
                val descriptor = JSONObject(descriptorJson)
                val kind = descriptor.getString("kind")
                if (kind == "image") {
                    ChatAttachment(
                        id = item.attachmentId,
                        kind = kind,
                        filename = descriptor.getString("originalFilename"),
                        mimeType = descriptor.getString("mimeType"),
                        sizeBytes = descriptor.getLong("sizeBytes"),
                        imageBytes = plaintext
                    )
                } else {
                    val file = writeAttachmentToCache(descriptor.getString("originalFilename"), plaintext)
                    ChatAttachment(
                        id = item.attachmentId,
                        kind = kind,
                        filename = descriptor.getString("originalFilename"),
                        mimeType = descriptor.getString("mimeType"),
                        sizeBytes = descriptor.getLong("sizeBytes"),
                        localPath = file.absolutePath
                    )
                }
            } catch (e: Exception) {
                Log.e("E2EE", "attachment decrypt failed msg=${msg.messageId} att=${item.attachmentId}", e)
                ChatAttachment(
                    id = item.attachmentId,
                    kind = "file",
                    filename = item.filename,
                    mimeType = item.mimeType,
                    sizeBytes = item.sizeBytes,
                    decryptionError = true
                )
            }
        }
    }

    private suspend fun materializeArchivedAttachment(
        record: com.example.messenger.proto.HistoryArchiveRecord,
        payload: JSONObject
    ): ChatAttachment {
        val descriptor = payload.getJSONObject("descriptor")
        val mediaId = payload.getString("mediaId")
        val media = repo!!.getMedia(mediaId)
        val plaintext = E2EE.decryptMedia(media.ciphertext.toByteArray(), media.nonce.toByteArray(), descriptor.toString())
        val kind = descriptor.getString("kind")
        val filename = descriptor.getString("originalFilename")
        val mimeType = descriptor.getString("mimeType")
        val sizeBytes = descriptor.getLong("sizeBytes")
        return if (kind == "image") {
            ChatAttachment(
                id = payload.optString("attachmentId", record.attachmentId),
                kind = kind,
                filename = filename,
                mimeType = mimeType,
                sizeBytes = sizeBytes,
                imageBytes = plaintext
            )
        } else {
            val file = writeAttachmentToCache(filename, plaintext)
            ChatAttachment(
                id = payload.optString("attachmentId", record.attachmentId),
                kind = kind,
                filename = filename,
                mimeType = mimeType,
                sizeBytes = sizeBytes,
                localPath = file.absolutePath
            )
        }
    }

    private suspend fun decryptGroupMsg(msg: Message, identity: IdentityState): String {
        val key = "${msg.conversationId}:${msg.conversationKeyVersion}"
        val groupKey = groupKeys[key] ?: fetchAndDecryptGroupKey(
            msg.conversationId, msg.conversationKeyVersion, identity
        ).also { groupKeys[key] = it }
        return E2EE.decryptGroupMessage(msg.ciphertext.toByteArray(), msg.nonce.toByteArray(), groupKey)
    }

    private suspend fun fetchAndDecryptGroupKey(
        conversationId: String,
        version: Int,
        identity: IdentityState
    ): ByteArray {
        val convKey = repo!!.getConversationKey(conversationId, version)
        val myEnvelope = findConversationEnvelopeForDevice(
            convKey.envelopesList,
            username = myUsername,
            deviceId = myDeviceId
        )
            ?: error("No envelope for $myUsername")
        val senderIdentKey = fetchSenderIdentity(convKey.createdBy, myEnvelope.senderKeyId)
        return E2EE.decryptGroupEnvelope(
            myEnvelope.encryptedKey.toByteArray(),
            myEnvelope.nonce.toByteArray(),
            identity.identityPrivateKey(),
            senderIdentKey
        )
    }

    private suspend fun decryptDirectMsg(msg: Message, identity: IdentityState, decryptAsSender: Boolean): String =
        if (decryptAsSender) decryptOwnDirectCiphertext(msg, identity) else decryptIncomingDirectCiphertext(msg, identity)

    private suspend fun decryptIncomingDirectCiphertext(msg: Message, identity: IdentityState): String {
        val senderPub = fetchSenderIdentity(msg.from, msg.senderKeyId)
        val otpPriv = msg.recipientOneTimePrekeyId
            .takeIf { it.isNotEmpty() }
            ?.let { id -> identity.findOtp(id)?.privateBytes?.let { E2EE.importPrivateKey(it) } }
        return E2EE.decryptDirectMessage(
            msg.ciphertext.toByteArray(),
            msg.nonce.toByteArray(),
            identity.signedPrekeyPrivateKey(),
            otpPriv,
            senderPub
        )
    }

    private fun decryptOwnDirectCiphertext(msg: Message, identity: IdentityState): String {
        val spkPub = msg.recipientSignedPrekeyPublic.toByteArray()
        val otpPub = msg.recipientOneTimePrekeyPublic.takeIf { !it.isEmpty }?.toByteArray()
        val key = E2EE.run {
            val priv = identity.identityPrivateKey()
            val secrets = mutableListOf(ecdh(priv, importPublicKey(spkPub)))
            if (otpPub != null) secrets += ecdh(priv, importPublicKey(otpPub))
            hkdf(secrets.reduce { a, b -> a + b }, info = "messenger-direct-prekey-v1")
        }
        return String(E2EE.aesGcmDecrypt(key, msg.ciphertext.toByteArray(), msg.nonce.toByteArray()), Charsets.UTF_8)
    }

    private fun decryptOwnDescriptor(msg: Message, item: Attachment, identity: IdentityState): String {
        val key = E2EE.run {
            val priv = identity.identityPrivateKey()
            val secrets = mutableListOf(ecdh(priv, importPublicKey(msg.recipientSignedPrekeyPublic.toByteArray())))
            if (!msg.recipientOneTimePrekeyPublic.isEmpty) {
                secrets += ecdh(priv, importPublicKey(msg.recipientOneTimePrekeyPublic.toByteArray()))
            }
            hkdf(secrets.reduce { a, b -> a + b }, info = "messenger-direct-prekey-v1")
        }
        return String(E2EE.aesGcmDecrypt(key, item.encryptedDescriptor.toByteArray(), item.descriptorNonce.toByteArray()), Charsets.UTF_8)
    }

    private suspend fun decryptIncomingDescriptor(msg: Message, item: Attachment, identity: IdentityState): String {
        val senderPub = fetchSenderIdentity(msg.from, msg.senderKeyId)
        val otpPriv = msg.recipientOneTimePrekeyId
            .takeIf { it.isNotEmpty() }
            ?.let { id -> identity.findOtp(id)?.privateBytes?.let { E2EE.importPrivateKey(it) } }
        return E2EE.decryptDirectMessage(
            item.encryptedDescriptor.toByteArray(),
            item.descriptorNonce.toByteArray(),
            identity.signedPrekeyPrivateKey(),
            otpPriv,
            senderPub
        )
    }

    private suspend fun fetchSenderIdentity(username: String, keyId: String): ByteArray {
        val cacheKey = "$username|$keyId"
        senderPubCache[cacheKey]?.let { return it }
        val key = repo!!.getIdentityKeys(listOf(username))
            .firstOrNull { it.username == username && it.keyId == keyId }
            ?: repo!!.getIdentityKey(username)
        return key.publicKey.toByteArray().also {
            senderPubCache[cacheKey] = it
        }
    }

    private fun buildDirectMessageEnvelopes(
        identity: IdentityState,
        plaintext: String,
        targets: List<DirectBundleTarget>,
    ): List<DirectMessageEnvelope> = targets.map { target ->
        val encrypted = E2EE.encryptDirectMessage(
            plaintext = plaintext,
            senderIdentPriv = identity.identityPrivateKey(),
            recipientSpkPub = target.signedPrekeyPublic,
            recipientOtpPub = target.oneTimePrekeyPublic,
        )
        DirectMessageEnvelope.newBuilder()
            .setTargetUsername(target.username)
            .setTargetDeviceId(target.deviceId)
            .setCiphertext(ByteString.copyFrom(encrypted.first))
            .setNonce(ByteString.copyFrom(encrypted.second))
            .setRecipientSignedPrekeyId(target.signedPrekeyId)
            .setRecipientSignedPrekeyPublic(ByteString.copyFrom(target.signedPrekeyPublic))
            .apply {
                if (target.oneTimePrekeyId.isNotBlank()) {
                    recipientOneTimePrekeyId = target.oneTimePrekeyId
                }
                target.oneTimePrekeyPublic?.let {
                    recipientOneTimePrekeyPublic = ByteString.copyFrom(it)
                }
            }
            .build()
    }

    private fun buildDirectAttachmentEnvelopes(
        identity: IdentityState,
        descriptorJson: String,
        targets: List<DirectBundleTarget>,
    ): List<AttachmentDirectEnvelope> = targets.map { target ->
        val encrypted = E2EE.encryptDirectMessage(
            plaintext = descriptorJson,
            senderIdentPriv = identity.identityPrivateKey(),
            recipientSpkPub = target.signedPrekeyPublic,
            recipientOtpPub = target.oneTimePrekeyPublic,
        )
        AttachmentDirectEnvelope.newBuilder()
            .setTargetUsername(target.username)
            .setTargetDeviceId(target.deviceId)
            .setEncryptedDescriptor(ByteString.copyFrom(encrypted.first))
            .setDescriptorNonce(ByteString.copyFrom(encrypted.second))
            .setRecipientSignedPrekeyId(target.signedPrekeyId)
            .setRecipientSignedPrekeyPublic(ByteString.copyFrom(target.signedPrekeyPublic))
            .apply {
                if (target.oneTimePrekeyId.isNotBlank()) {
                    recipientOneTimePrekeyId = target.oneTimePrekeyId
                }
                target.oneTimePrekeyPublic?.let {
                    recipientOneTimePrekeyPublic = ByteString.copyFrom(it)
                }
            }
            .build()
    }

    fun sendDirect(toUsername: String, text: String, attachmentUri: Uri? = null) {
        viewModelScope.launch {
            try {
                val identity = app.identityStore.load() ?: error("Нет E2EE ключей")
                val recipientBundles = repo!!.acquirePrekeyBundles(toUsername)
                if (recipientBundles.isEmpty()) {
                    error("prekey bundle not found")
                }
                val ownBundles = repo!!.acquirePrekeyBundles(myUsername)
                val targets = buildDirectBundleTargets(
                    currentUsername = myUsername,
                    currentDeviceId = myDeviceId,
                    localSignedPrekeyId = identity.signedPrekeyId,
                    localSignedPrekeyPublic = identity.signedPrekeyPublicBytes,
                    recipientBundles = recipientBundles,
                    ownBundles = ownBundles,
                )
                val directEnvelopes = text.takeIf { it.isNotBlank() }?.let {
                    buildDirectMessageEnvelopes(identity, it, targets)
                } ?: emptyList()
                val attachments = attachmentUri?.let {
                    listOf(prepareDirectAttachment(it, identity, targets))
                } ?: emptyList()
                val createdAt = Instant.now()
                val archiveRecords = buildArchiveRecordsForDirect(
                    participants = listOf(myUsername, toUsername),
                    conversationId = activeConversationId,
                    text = text,
                    attachments = attachments,
                    createdAt = createdAt,
                )
                repo!!.sendMessageEncryptedDirect(
                    to = toUsername,
                    senderKeyId = identity.keyId,
                    directEnvelopes = directEnvelopes,
                    attachments = attachments.map { it.proto },
                    archiveRecords = archiveRecords,
                )
            } catch (e: Exception) {
                val msg = if (e.message?.contains("prekey bundle not found") == true || e.message?.contains("identity key not found") == true)
                    "Собеседник не опубликовал E2EE-ключи — попросите его зайти в систему."
                else e.message ?: "Ошибка отправки"
                _state.value = _state.value.copy(sendError = msg)
            }
        }
    }

    fun sendGroup(conversationId: String, text: String, attachmentUri: Uri? = null) {
        viewModelScope.launch {
            try {
                val identity = app.identityStore.load() ?: error("Нет E2EE ключей")
                val convKey = repo!!.getConversationKey(conversationId, 0)
                val cacheKey = "${conversationId}:${convKey.version}"
                val groupKey = groupKeys[cacheKey] ?: fetchAndDecryptGroupKey(
                    conversationId, convKey.version, identity
                ).also { groupKeys[cacheKey] = it }
                val encryptedText = text.takeIf { it.isNotBlank() }?.let { E2EE.encryptGroupMessage(it, groupKey) }
                val attachments = attachmentUri?.let {
                    listOf(prepareGroupAttachment(it, groupKey))
                } ?: emptyList()
                val createdAt = Instant.now()
                val participants = convKey.envelopesList.map { it.username }.distinct()
                val archiveRecords = buildArchiveRecordsForGroup(
                    participants = participants,
                    conversationId = conversationId,
                    text = text,
                    attachments = attachments,
                    createdAt = createdAt,
                    conversationKeyVersion = convKey.version,
                )
                repo!!.sendMessageEncryptedGroup(
                    conversationId = conversationId,
                    ciphertext = encryptedText?.first ?: byteArrayOf(),
                    nonce = encryptedText?.second ?: byteArrayOf(),
                    senderKeyId = identity.keyId,
                    keyVersion = convKey.version,
                    attachments = attachments.map { it.proto },
                    archiveRecords = archiveRecords,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(sendError = e.message ?: "Ошибка отправки")
            }
        }
    }

    fun clearSendError() {
        _state.value = _state.value.copy(sendError = null)
    }

    private suspend fun buildArchiveRecordsForDirect(
        participants: List<String>,
        conversationId: String,
        text: String,
        attachments: List<PreparedAttachment>,
        createdAt: Instant,
    ): List<com.example.messenger.proto.HistoryArchiveRecord> {
        val records = mutableListOf<com.example.messenger.proto.HistoryArchiveRecord>()
        if (text.isNotBlank()) {
            records += buildArchiveRecords(
                participants = participants,
                conversationId = conversationId,
                recordType = HistoryArchiveRecordType.HISTORY_ARCHIVE_RECORD_TYPE_DIRECT_MESSAGE,
                payload = JSONObject().apply {
                    put("kind", "message")
                    put("from", myUsername)
                    put("to", participants.firstOrNull { it != myUsername } ?: "")
                    put("text", text)
                    put("senderDeviceId", myDeviceId)
                    put("createdAt", createdAt.toString())
                    put("conversationKeyVersion", 1)
                },
                createdAt = createdAt,
            )
        }
        for (attachment in attachments) {
            records += buildArchiveRecords(
                participants = participants,
                conversationId = conversationId,
                recordType = HistoryArchiveRecordType.HISTORY_ARCHIVE_RECORD_TYPE_DIRECT_ATTACHMENT_DESCRIPTOR,
                payload = JSONObject().apply {
                    put("kind", "attachment")
                    put("attachmentId", attachment.proto.attachmentId)
                    put("from", myUsername)
                    put("to", participants.firstOrNull { it != myUsername } ?: "")
                    put("text", text)
                    put("senderDeviceId", myDeviceId)
                    put("createdAt", createdAt.toString())
                    put("conversationKeyVersion", 1)
                    put("mediaId", attachment.mediaId)
                    put("descriptor", JSONObject(attachment.descriptorJson))
                    put("sha256", byteArrayToJsonArray(attachment.proto.sha256.toByteArray()))
                },
                createdAt = createdAt,
                attachmentId = attachment.proto.attachmentId,
            )
        }
        return records
    }

    private suspend fun buildArchiveRecordsForGroup(
        participants: List<String>,
        conversationId: String,
        text: String,
        attachments: List<PreparedAttachment>,
        createdAt: Instant,
        conversationKeyVersion: Int,
    ): List<com.example.messenger.proto.HistoryArchiveRecord> {
        val records = mutableListOf<com.example.messenger.proto.HistoryArchiveRecord>()
        if (text.isNotBlank()) {
            records += buildArchiveRecords(
                participants = participants,
                conversationId = conversationId,
                recordType = HistoryArchiveRecordType.HISTORY_ARCHIVE_RECORD_TYPE_GROUP_MESSAGE,
                payload = JSONObject().apply {
                    put("kind", "message")
                    put("from", myUsername)
                    put("to", conversationId)
                    put("text", text)
                    put("senderDeviceId", myDeviceId)
                    put("createdAt", createdAt.toString())
                    put("conversationKeyVersion", conversationKeyVersion)
                },
                createdAt = createdAt,
            )
        }
        for (attachment in attachments) {
            records += buildArchiveRecords(
                participants = participants,
                conversationId = conversationId,
                recordType = HistoryArchiveRecordType.HISTORY_ARCHIVE_RECORD_TYPE_GROUP_ATTACHMENT_DESCRIPTOR,
                payload = JSONObject().apply {
                    put("kind", "attachment")
                    put("attachmentId", attachment.proto.attachmentId)
                    put("from", myUsername)
                    put("to", conversationId)
                    put("text", text)
                    put("senderDeviceId", myDeviceId)
                    put("createdAt", createdAt.toString())
                    put("conversationKeyVersion", conversationKeyVersion)
                    put("mediaId", attachment.mediaId)
                    put("descriptor", JSONObject(attachment.descriptorJson))
                    put("sha256", byteArrayToJsonArray(attachment.proto.sha256.toByteArray()))
                },
                createdAt = createdAt,
                attachmentId = attachment.proto.attachmentId,
            )
        }
        return records
    }

    private suspend fun buildArchiveRecords(
        participants: List<String>,
        conversationId: String,
        recordType: HistoryArchiveRecordType,
        payload: JSONObject,
        createdAt: Instant,
        attachmentId: String = "",
    ): List<com.example.messenger.proto.HistoryArchiveRecord> {
        val archiveIdentity = app.archiveStore.load() ?: return emptyList()
        val pubKeys = runCatching { repo!!.getArchivePublicKeys(participants.distinct()) }.getOrElse {
            Log.w("Archive", "fetch public keys failed: ${it.message}")
            return emptyList()
        }
        val timestamp = Timestamp.newBuilder().setSeconds(createdAt.epochSecond).build()
        return pubKeys.mapNotNull { bundle ->
            if (bundle.publicKey.isEmpty) {
                return@mapNotNull null
            }
            runCatching {
                val enc = ArchiveE2EE.encryptPayload(payload.toString().toByteArray(Charsets.UTF_8), bundle.publicKey.toByteArray())
                historyArchiveRecord {
                    recordId = UUID.randomUUID().toString()
                    ownerUsername = bundle.username
                    this.conversationId = conversationId
                    this.recordType = recordType
                    this.attachmentId = attachmentId
                    this.sender = myUsername
                    this.createdAt = timestamp
                    ciphertext = ByteString.copyFrom(enc.ciphertext)
                    nonce = ByteString.copyFrom(enc.nonce)
                    ephemeralPublicKey = ByteString.copyFrom(enc.ephemeralPublicKey)
                    archiveKeyVersion = archiveIdentity.version
                }
            }.getOrElse {
                Log.w("Archive", "encrypt record for ${bundle.username} failed: ${it.message}")
                null
            }
        }
    }

    private suspend fun prepareDirectAttachment(uri: Uri, identity: IdentityState, targets: List<DirectBundleTarget>): PreparedAttachment {
        val selected = readSelectedAttachment(uri)
        val encrypted = E2EE.encryptMedia(selected.bytes, selected.mimeType, selected.filename, selected.kindName)
        val prepared = repo!!.prepareMediaUpload(selected.filename, selected.mimeType, selected.bytes.size.toLong(), selected.kind)
        repo!!.uploadMedia(
            mediaId = prepared.mediaId,
            ciphertext = encrypted.ciphertext,
            nonce = encrypted.nonce,
            sha256 = encrypted.sha256,
            sizeBytes = selected.bytes.size.toLong(),
            mimeType = selected.mimeType,
            filename = selected.filename,
            kind = selected.kind
        )
        val descriptorEnvelopes = buildDirectAttachmentEnvelopes(identity, encrypted.descriptorJson, targets)
        val proto = Attachment.newBuilder()
            .setAttachmentId(UUID.randomUUID().toString())
            .setKind(selected.kind)
            .setFilename(selected.filename)
            .setMimeType(selected.mimeType)
            .setSizeBytes(selected.bytes.size.toLong())
            .setMediaId(prepared.mediaId)
            .setSha256(ByteString.copyFrom(encrypted.sha256))
            .setCiphertextSize(encrypted.ciphertext.size.toLong())
            .setPreview(AttachmentPreview.newBuilder().setWidth(0).setHeight(0).build())
            .addAllDirectEnvelopes(descriptorEnvelopes)
            .build()
        return PreparedAttachment(
            proto = proto,
            descriptorJson = encrypted.descriptorJson,
            mediaId = prepared.mediaId,
            filename = selected.filename,
            mimeType = selected.mimeType,
            kindName = selected.kindName,
        )
    }

    private suspend fun prepareGroupAttachment(uri: Uri, groupKey: ByteArray): PreparedAttachment {
        val selected = readSelectedAttachment(uri)
        val encrypted = E2EE.encryptMedia(selected.bytes, selected.mimeType, selected.filename, selected.kindName)
        val prepared = repo!!.prepareMediaUpload(selected.filename, selected.mimeType, selected.bytes.size.toLong(), selected.kind)
        repo!!.uploadMedia(
            mediaId = prepared.mediaId,
            ciphertext = encrypted.ciphertext,
            nonce = encrypted.nonce,
            sha256 = encrypted.sha256,
            sizeBytes = selected.bytes.size.toLong(),
            mimeType = selected.mimeType,
            filename = selected.filename,
            kind = selected.kind
        )
        val descriptor = E2EE.encryptGroupMessage(encrypted.descriptorJson, groupKey)
        val proto = Attachment.newBuilder()
            .setAttachmentId(UUID.randomUUID().toString())
            .setKind(selected.kind)
            .setFilename(selected.filename)
            .setMimeType(selected.mimeType)
            .setSizeBytes(selected.bytes.size.toLong())
            .setMediaId(prepared.mediaId)
            .setEncryptedDescriptor(ByteString.copyFrom(descriptor.first))
            .setDescriptorNonce(ByteString.copyFrom(descriptor.second))
            .setSha256(ByteString.copyFrom(encrypted.sha256))
            .setCiphertextSize(encrypted.ciphertext.size.toLong())
            .setPreview(AttachmentPreview.newBuilder().setWidth(0).setHeight(0).build())
            .build()
        return PreparedAttachment(
            proto = proto,
            descriptorJson = encrypted.descriptorJson,
            mediaId = prepared.mediaId,
            filename = selected.filename,
            mimeType = selected.mimeType,
            kindName = selected.kindName,
        )
    }

    private data class SelectedAttachment(
        val filename: String,
        val mimeType: String,
        val kind: AttachmentKind,
        val kindName: String,
        val bytes: ByteArray
    )

    private fun readSelectedAttachment(uri: Uri): SelectedAttachment {
        val resolver = getApplication<Application>().contentResolver
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        val filename = resolveDisplayName(uri) ?: "attachment"
        val kind = when {
            mimeType.startsWith("image/") -> AttachmentKind.ATTACHMENT_KIND_IMAGE
            mimeType.startsWith("video/") -> AttachmentKind.ATTACHMENT_KIND_VIDEO
            else -> AttachmentKind.ATTACHMENT_KIND_FILE
        }
        val kindName = when (kind) {
            AttachmentKind.ATTACHMENT_KIND_IMAGE -> "image"
            AttachmentKind.ATTACHMENT_KIND_VIDEO -> "video"
            else -> "file"
        }
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Не удалось прочитать файл")
        require(bytes.size <= 25 * 1024 * 1024) { "Файл больше 25 МБ" }
        return SelectedAttachment(filename, mimeType, kind, kindName, bytes)
    }

    private fun resolveDisplayName(uri: Uri): String? {
        val resolver = getApplication<Application>().contentResolver
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        return null
    }

    private fun writeAttachmentToCache(filename: String, bytes: ByteArray): File {
        val safeName = filename.ifBlank { "attachment" }
        val file = File(getApplication<Application>().cacheDir, "att-${UUID.randomUUID()}-$safeName")
        file.writeBytes(bytes)
        return file
    }
}

internal fun mergeArchivedMessages(live: List<ChatMessage>, archived: List<ChatMessage>): List<ChatMessage> {
    if (archived.isEmpty()) {
        return live
    }
    val merged = linkedMapOf<Long, ChatMessage>()
    for (message in live) {
        merged[message.id] = message
    }
    for (archivedMessage in archived) {
        val existing = merged[archivedMessage.id]
        if (existing == null) {
            merged[archivedMessage.id] = archivedMessage
            continue
        }
        val shouldReplaceText = existing.text == "[Сообщение недоступно на этом устройстве]" ||
            (existing.decryptionError && archivedMessage.text.isNotBlank())
        val shouldReplaceAttachments = existing.attachments.isEmpty() ||
            (existing.attachments.all { it.decryptionError } && archivedMessage.attachments.isNotEmpty())
        merged[archivedMessage.id] = existing.copy(
            text = if (shouldReplaceText) archivedMessage.text else existing.text,
            decryptionError = if (shouldReplaceText) false else existing.decryptionError,
            attachments = if (shouldReplaceAttachments) archivedMessage.attachments else existing.attachments,
        )
    }
    return merged.values.sortedBy { it.id }
}

private fun parseArchiveCreatedAt(payload: JSONObject, record: com.example.messenger.proto.HistoryArchiveRecord): Long {
    val raw = payload.optString("createdAt")
    if (raw.isNotBlank()) {
        return runCatching { Instant.parse(raw).epochSecond }.getOrDefault(record.createdAt.seconds)
    }
    return record.createdAt.seconds
}

private fun byteArrayToJsonArray(bytes: ByteArray): JSONArray = JSONArray().apply {
    bytes.forEach { put(it.toInt() and 0xff) }
}

private fun jsonArrayToByteArray(array: JSONArray?): ByteArray {
    if (array == null) {
        return byteArrayOf()
    }
    return ByteArray(array.length()) { index -> array.optInt(index).toByte() }
}

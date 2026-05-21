package com.example.messenger.data

import com.example.messenger.proto.ArchiveKeyBundle
import com.example.messenger.proto.Attachment
import com.example.messenger.proto.AttachmentDirectEnvelope
import com.example.messenger.proto.AttachmentKind
import com.example.messenger.proto.Conversation
import com.example.messenger.proto.ConversationKey
import com.example.messenger.proto.ConversationKeyEnvelope
import com.example.messenger.proto.DirectMessageEnvelope
import com.example.messenger.proto.HistoryArchiveHeader
import com.example.messenger.proto.HistoryArchiveRecord
import com.example.messenger.proto.IdentityKey
import com.example.messenger.proto.Message
import com.example.messenger.proto.OneTimePrekeyUpload
import com.example.messenger.proto.PrekeyBundle
import com.example.messenger.proto.Profile
import com.example.messenger.proto.ServerEvent
import com.example.messenger.proto.acquirePrekeyBundleRequest
import com.example.messenger.proto.acquirePrekeyBundlesRequest
import com.example.messenger.proto.appendHistoryArchiveRecordsRequest
import com.example.messenger.proto.getArchivePublicKeysRequest
import com.example.messenger.proto.getConversationKeyRequest
import com.example.messenger.proto.getIdentityKeyRequest
import com.example.messenger.proto.getIdentityKeysRequest
import com.example.messenger.proto.getMediaRequest
import com.example.messenger.proto.getMessagesRequest
import com.example.messenger.proto.getProfileRequest
import com.example.messenger.proto.initializeHistoryArchiveRequest
import com.example.messenger.proto.listHistoryArchiveRecordsRequest
import com.example.messenger.proto.prepareMediaUploadRequest
import com.example.messenger.proto.publishIdentityKeyRequest
import com.example.messenger.proto.publishPrekeyBundleRequest
import com.example.messenger.proto.searchUsersRequest
import com.example.messenger.proto.sendMessageRequest
import com.example.messenger.proto.streamEventsRequest
import com.example.messenger.proto.uploadMediaRequest
import com.example.messenger.proto.upsertConversationKeyRequest
import com.google.protobuf.ByteString
import com.google.protobuf.Empty
import kotlinx.coroutines.flow.Flow

class MessengerRepository(private val grpc: GrpcManager, private val token: String) {

    // ── Conversations ────────────────────────────────────────────────────────

    suspend fun listConversations(): List<Conversation> =
        grpc.userStub(token).listConversations(Empty.getDefaultInstance()).itemsList

    suspend fun searchUsers(query: String): List<Profile> =
        grpc.userStub(token).searchUsers(searchUsersRequest {
            this.query = query
            limit = 20
        }).itemsList

    suspend fun getProfile(username: String): Profile =
        grpc.userStub(token).getProfile(getProfileRequest { this.username = username })

    // ── Messages ─────────────────────────────────────────────────────────────

    suspend fun getMessages(conversationId: String? = null, withUsername: String? = null, limit: Int = 100): List<Message> =
        grpc.messageStub(token).getMessages(getMessagesRequest {
            if (conversationId != null) this.conversationId = conversationId
            if (withUsername != null) this.withUsername = withUsername
            this.limit = limit
        }).itemsList

    suspend fun sendMessagePlain(to: String, text: String, conversationId: String = "") {
        grpc.messageStub(token).sendMessage(sendMessageRequest {
            this.to = to
            this.text = text
            if (conversationId.isNotEmpty()) this.conversationId = conversationId
        })
    }

    suspend fun prepareMediaUpload(filename: String, mimeType: String, sizeBytes: Long, kind: AttachmentKind) =
        grpc.messageStub(token).prepareMediaUpload(prepareMediaUploadRequest {
            this.filename = filename
            this.mimeType = mimeType
            this.sizeBytes = sizeBytes
            this.kind = kind
        })

    suspend fun uploadMedia(
        mediaId: String,
        ciphertext: ByteArray,
        nonce: ByteArray,
        sha256: ByteArray,
        sizeBytes: Long,
        mimeType: String,
        filename: String,
        kind: AttachmentKind
    ) = grpc.messageStub(token).uploadMedia(uploadMediaRequest {
        this.mediaId = mediaId
        this.ciphertext = ByteString.copyFrom(ciphertext)
        this.nonce = ByteString.copyFrom(nonce)
        this.sha256 = ByteString.copyFrom(sha256)
        this.sizeBytes = sizeBytes
        this.mimeType = mimeType
        this.filename = filename
        this.kind = kind
    })

    suspend fun getMedia(mediaId: String) =
        grpc.messageStub(token).getMedia(getMediaRequest { this.mediaId = mediaId })

    suspend fun sendMessageEncryptedDirect(
        to: String,
        senderKeyId: String,
        directEnvelopes: List<DirectMessageEnvelope>,
        attachments: List<Attachment> = emptyList(),
        archiveRecords: List<HistoryArchiveRecord> = emptyList()
    ) {
        grpc.messageStub(token).sendMessage(sendMessageRequest {
            this.to = to
            this.senderKeyId = senderKeyId
            this.encrypted = true
            this.directEnvelopes.addAll(directEnvelopes)
            this.attachments.addAll(attachments)
            this.archiveRecords.addAll(archiveRecords)
        })
    }

    suspend fun sendMessageEncryptedGroup(
        conversationId: String,
        ciphertext: ByteArray,
        nonce: ByteArray,
        senderKeyId: String,
        keyVersion: Int,
        attachments: List<Attachment> = emptyList(),
        archiveRecords: List<HistoryArchiveRecord> = emptyList()
    ) {
        grpc.messageStub(token).sendMessage(sendMessageRequest {
            this.conversationId = conversationId
            this.ciphertext = ByteString.copyFrom(ciphertext)
            this.nonce = ByteString.copyFrom(nonce)
            this.senderKeyId = senderKeyId
            this.conversationKeyVersion = keyVersion
            this.encrypted = true
            this.attachments.addAll(attachments)
            this.archiveRecords.addAll(archiveRecords)
        })
    }

    fun streamEvents(): Flow<ServerEvent> =
        grpc.messageStub(token).streamEvents(streamEventsRequest { })

    // ── Keys ─────────────────────────────────────────────────────────────────

    suspend fun publishIdentityKey(keyId: String, publicKeyBytes: ByteArray): IdentityKey =
        grpc.userStub(token).publishIdentityKey(publishIdentityKeyRequest {
            this.keyId = keyId
            algorithm = "Ed25519"
            publicKey = ByteString.copyFrom(publicKeyBytes)
        })

    suspend fun publishPrekeyBundle(
        spkId: String,
        spkPub: ByteArray,
        spkSignature: ByteArray,
        otps: List<Pair<String, ByteArray>>
    ): PrekeyBundle = grpc.userStub(token).publishPrekeyBundle(publishPrekeyBundleRequest {
        signedPrekeyId = spkId
        signedPrekeyAlgorithm = "X25519"
        signedPrekeyPublicKey = ByteString.copyFrom(spkPub)
        signedPrekeySignature = ByteString.copyFrom(spkSignature)
        signedPrekeySignatureAlgorithm = "Ed25519"
        oneTimePrekeys.addAll(otps.map { (id, pub) ->
            OneTimePrekeyUpload.newBuilder()
                .setKeyId(id)
                .setAlgorithm("P256-HKDF-AESGCM")
                .setPublicKey(ByteString.copyFrom(pub))
                .build()
        })
    })

    suspend fun getIdentityKey(username: String, deviceId: String = ""): IdentityKey =
        grpc.userStub(token).getIdentityKey(getIdentityKeyRequest {
            this.username = username
            if (deviceId.isNotBlank()) {
                this.deviceId = deviceId
            }
        })

    suspend fun getIdentityKeys(usernames: List<String>): List<IdentityKey> =
        grpc.userStub(token).getIdentityKeys(getIdentityKeysRequest {
            this.usernames.addAll(usernames)
        }).itemsList

    suspend fun acquirePrekeyBundle(username: String): PrekeyBundle =
        grpc.userStub(token).acquirePrekeyBundle(acquirePrekeyBundleRequest { this.username = username })

    suspend fun acquirePrekeyBundles(username: String): List<PrekeyBundle> =
        grpc.userStub(token).acquirePrekeyBundles(acquirePrekeyBundlesRequest {
            this.username = username
        }).itemsList

    suspend fun getConversationKey(conversationId: String, version: Int = 0): ConversationKey =
        grpc.userStub(token).getConversationKey(getConversationKeyRequest {
            this.conversationId = conversationId
            this.version = version
        })

    suspend fun upsertConversationKey(
        conversationId: String,
        version: Int,
        envelopes: List<ConversationKeyEnvelope>
    ): ConversationKey = grpc.userStub(token).upsertConversationKey(upsertConversationKeyRequest {
        this.conversationId = conversationId
        this.version = version
        algorithm = "AES-GCM"
        this.envelopes.addAll(envelopes)
    })

    // ── History archive ──────────────────────────────────────────────────────

    suspend fun initializeHistoryArchive(
        publicKey: ByteArray,
        encryptedPrivateKey: ByteArray,
        kdfSalt: ByteArray,
        kdfParams: String,
        version: Int
    ): HistoryArchiveHeader = grpc.userStub(token).initializeHistoryArchive(initializeHistoryArchiveRequest {
        this.publicKey = ByteString.copyFrom(publicKey)
        this.encryptedPrivateKey = ByteString.copyFrom(encryptedPrivateKey)
        this.kdfSalt = ByteString.copyFrom(kdfSalt)
        this.kdfParams = kdfParams
        this.version = version
    })

    suspend fun getHistoryArchiveHeader(): HistoryArchiveHeader =
        grpc.userStub(token).getHistoryArchiveHeader(Empty.getDefaultInstance())

    suspend fun getArchivePublicKeys(usernames: List<String>): List<ArchiveKeyBundle> =
        grpc.userStub(token).getArchivePublicKeys(getArchivePublicKeysRequest {
            this.usernames.addAll(usernames)
        }).itemsList

    suspend fun appendHistoryArchiveRecords(records: List<HistoryArchiveRecord>) {
        grpc.messageStub(token).appendHistoryArchiveRecords(appendHistoryArchiveRecordsRequest {
            items.addAll(records)
        })
    }

    suspend fun listHistoryArchiveRecords(afterSequence: Long = 0, limit: Int = 500): List<HistoryArchiveRecord> =
        grpc.messageStub(token).listHistoryArchiveRecords(listHistoryArchiveRecordsRequest {
            this.afterSequence = afterSequence
            this.limit = limit
        }).itemsList
}

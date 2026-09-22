package com.vyrncore.palestra.ui.chat

import android.content.ContentResolver
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vyrncore.palestra.data.local.entity.ChatAttachmentType
import com.vyrncore.palestra.data.local.entity.ChatMessageEntity
import com.vyrncore.palestra.data.repository.AuthRepository
import com.vyrncore.palestra.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Backs a single PT<->Allievo conversation thread. The peer is set imperatively via [setPeer]
 * rather than through SavedStateHandle nav args, so the same screen/VM can be embedded directly
 * as a bottom-nav tab (Allievo, single fixed peer = their PT) or pushed as a nav destination
 * (PT, one destination per client picked from a conversation list).
 */
@HiltViewModel
class ChatThreadViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: android.content.Context,
) : ViewModel() {

    val userId: String = authRepository.currentUserId.orEmpty()
    private val peerId = MutableStateFlow<String?>(null)

    fun setPeer(newPeerId: String) {
        if (peerId.value == newPeerId) return
        peerId.value = newPeerId
        chatRepository.setActiveConversation(newPeerId)
        viewModelScope.launch { chatRepository.markConversationRead(userId, newPeerId) }
    }

    val messages: StateFlow<List<ChatMessageEntity>> = peerId.filterNotNull()
        .flatMapLatest { peer -> chatRepository.observeConversation(userId, peer) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val peerName: StateFlow<String> = peerId.filterNotNull()
        .flatMapLatest { peer -> authRepository.observeProfile(peer) }
        .map { it?.fullName ?: "Chat" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Chat")

    val peerIsTyping: StateFlow<Boolean> = peerId.filterNotNull()
        .flatMapLatest { peer -> flow { emitAll(chatRepository.observeTyping(userId, peer)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private var typingIdleJob: Job? = null

    /** Broadcasts "typing" on every keystroke, then auto-clears it after a pause so the peer
     * doesn't see a stuck indicator if the allievo/PT stops typing without sending or leaving. */
    fun notifyTyping() {
        val peer = peerId.value ?: return
        viewModelScope.launch { chatRepository.sendTypingEvent(userId, peer, isTyping = true) }
        typingIdleJob?.cancel()
        typingIdleJob = viewModelScope.launch {
            delay(3000)
            chatRepository.sendTypingEvent(userId, peer, isTyping = false)
        }
    }

    fun sendMessage(content: String) {
        val peer = peerId.value ?: return
        if (content.isBlank()) return
        typingIdleJob?.cancel()
        viewModelScope.launch {
            chatRepository.sendTypingEvent(userId, peer, isTyping = false)
            chatRepository.sendMessage(userId, peer, content)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch { chatRepository.deleteMessage(messageId, userId) }
    }

    fun sendAttachment(uri: Uri) {
        val peer = peerId.value ?: return
        viewModelScope.launch {
            val resolver = context.contentResolver
            val mimeType = resolver.getType(uri).orEmpty()
            val fileName = queryFileName(resolver, uri) ?: "file"
            val bytes = withContext(Dispatchers.IO) {
                resolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: return@launch
            val type = if (mimeType.startsWith("image/")) ChatAttachmentType.IMAGE else ChatAttachmentType.FILE
            chatRepository.sendAttachment(userId, peer, fileName, bytes, type)
        }
    }

    private var recorder: MediaRecorder? = null
    private var recordingFile: java.io.File? = null
    private var recordingStartMs = 0L

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingSeconds = MutableStateFlow(0)
    val recordingSeconds: StateFlow<Int> = _recordingSeconds.asStateFlow()

    private var recordingTickJob: Job? = null

    /** Starts recording a voice message to a private cache file - call only after RECORD_AUDIO
     * has been granted (the screen requests it first). */
    fun startRecording() {
        if (_isRecording.value) return
        val file = java.io.File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION") MediaRecorder()
        }
        runCatching {
            rec.apply {
                // VOICE_COMMUNICATION (not the plain MIC source) is what actually made these
                // sound bad: it's the only source that gets the device's hardware/OEM noise
                // suppression, echo cancellation and automatic gain control applied to it, since
                // that's the path phone calls use. Plain MIC records the raw, unprocessed signal -
                // fine for music, thin and noisy for a voice memo held at arm's length.
                setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                // Left unset, these default to whatever the OEM's audio HAL picks - commonly a
                // low bitrate (sometimes under 24kbps) that makes speech sound compressed and
                // muffled. 44.1kHz/128kbps mono is well above what a human voice needs and keeps
                // clips small (roughly 1MB/min) while sounding clean.
                setAudioSamplingRate(44_100)
                setAudioEncodingBitRate(128_000)
                setAudioChannels(1)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        }.onSuccess {
            recorder = rec
            recordingFile = file
            recordingStartMs = System.currentTimeMillis()
            attachNoiseProcessing(rec.audioSessionId)
            _isRecording.value = true
            _recordingSeconds.value = 0
            recordingTickJob = viewModelScope.launch {
                while (true) {
                    delay(1000)
                    _recordingSeconds.value = ((System.currentTimeMillis() - recordingStartMs) / 1000).toInt()
                }
            }
        }.onFailure { rec.release() }
    }

    private var noiseSuppressor: android.media.audiofx.NoiseSuppressor? = null
    private var echoCanceler: android.media.audiofx.AcousticEchoCanceler? = null
    private var gainControl: android.media.audiofx.AutomaticGainControl? = null

    /** VOICE_COMMUNICATION already engages the device's audio HAL processing, but these
     * platform-level effects stack on top where the hardware supports them - most phones do, some
     * budget/older devices don't, hence the availability checks (each `create()` degrades to a
     * silent no-op object if the effect can't actually be applied on this device). */
    private fun attachNoiseProcessing(audioSessionId: Int) {
        runCatching {
            if (android.media.audiofx.NoiseSuppressor.isAvailable()) {
                noiseSuppressor = android.media.audiofx.NoiseSuppressor.create(audioSessionId)?.apply { enabled = true }
            }
            if (android.media.audiofx.AcousticEchoCanceler.isAvailable()) {
                echoCanceler = android.media.audiofx.AcousticEchoCanceler.create(audioSessionId)?.apply { enabled = true }
            }
            if (android.media.audiofx.AutomaticGainControl.isAvailable()) {
                gainControl = android.media.audiofx.AutomaticGainControl.create(audioSessionId)?.apply { enabled = true }
            }
        }
    }

    private fun releaseNoiseProcessing() {
        runCatching { noiseSuppressor?.release() }
        runCatching { echoCanceler?.release() }
        runCatching { gainControl?.release() }
        noiseSuppressor = null
        echoCanceler = null
        gainControl = null
    }

    /** Stops recording and sends the clip as a VOICE attachment - a tap under 1s is treated as an
     * accidental press and discarded instead of sending a near-silent blip. */
    fun stopRecordingAndSend() {
        val peer = peerId.value
        val file = recordingFile
        recordingTickJob?.cancel()
        val durationMs = System.currentTimeMillis() - recordingStartMs
        runCatching { recorder?.stop() }
        recorder?.release()
        recorder = null
        releaseNoiseProcessing()
        _isRecording.value = false
        _recordingSeconds.value = 0
        if (peer == null || file == null || durationMs < 1000) {
            file?.delete()
            return
        }
        viewModelScope.launch {
            val bytes = withContext(Dispatchers.IO) { file.readBytes() }
            file.delete()
            chatRepository.sendAttachment(userId, peer, "voice.m4a", bytes, ChatAttachmentType.VOICE)
        }
    }

    fun cancelRecording() {
        recordingTickJob?.cancel()
        runCatching { recorder?.stop() }
        recorder?.release()
        recorder = null
        releaseNoiseProcessing()
        _isRecording.value = false
        _recordingSeconds.value = 0
        recordingFile?.delete()
        recordingFile = null
    }

    private fun queryFileName(resolver: ContentResolver, uri: Uri): String? {
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }
        return null
    }

    override fun onCleared() {
        super.onCleared()
        chatRepository.setActiveConversation(null)
        chatRepository.stopTypingChannel()
        if (_isRecording.value) cancelRecording()
    }
}

package com.audiochat

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.*

class MainActivity : AppCompatActivity() {
    private lateinit var usernameInput: EditText
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var chatView: TextView
    private lateinit var scrollView: ScrollView
    
    private var audioChat: AudioChat? = null
    private var isListening = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        usernameInput = findViewById(R.id.usernameInput)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        chatView = findViewById(R.id.chatView)
        scrollView = findViewById(R.id.scrollView)
        
        // Solicitar permisos
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
        
        sendButton.setOnClickListener {
            val username = usernameInput.text.toString().take(16)
            val message = messageInput.text.toString().take(64)
            
            if (username.isNotEmpty() && message.isNotEmpty()) {
                if (audioChat == null) {
                    audioChat = AudioChat(username) { sender, msg ->
                        runOnUiThread {
                            appendMessage(sender, msg)
                        }
                    }
                    audioChat?.start()
                    isListening = true
                }
                
                audioChat?.sendMessage(message)
                messageInput.text.clear()
                
                // Mostrar mensaje propio
                appendMessage(username, message)
            }
        }
    }
    
    private fun appendMessage(sender: String, message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val formatted = "[$time] $sender: $message\n"
        chatView.append(formatted)
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        audioChat?.stop()
    }
}

class AudioChat(
    private val username: String,
    private val onMessageReceived: (String, String) -> Unit
) {
    private val sampleRate = 44100
    private val bitDuration = 0.004 // 4ms
    private val samplesPerBit = (sampleRate * bitDuration).toInt()
    
    // 8 frecuencias ultrasónicas
    private val freqs = (0..7).map { 17000 + it * 485 }
    
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var isRunning = false
    private val buffer = mutableListOf<Float>()
    
    fun start() {
        isRunning = true
        
        // Inicializar AudioRecord
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        
        // Inicializar AudioTrack
        audioTrack = AudioTrack(
            android.media.AudioManager.STREAM_MUSIC,
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
            AudioTrack.MODE_STREAM
        )
        
        audioRecord?.startRecording()
        audioTrack?.play()
        
        // Thread de escucha
        thread { listenLoop() }
    }
    
    private fun listenLoop() {
        val readBuffer = ShortArray(samplesPerBit * 4)
        
        while (isRunning) {
            val read = audioRecord?.read(readBuffer, 0, readBuffer.size) ?: 0
            if (read > 0) {
                // Convertir a float y agregar al buffer
                val floatData = readBuffer.take(read).map { it / 32768f }
                buffer.addAll(floatData)
                
                // Procesar buffer
                if (buffer.size >= samplesPerBit * 30) {
                    processBuffer()
                    
                    // Limpiar buffer viejo
                    if (buffer.size > samplesPerBit * 100) {
                        buffer.subList(0, buffer.size - samplesPerBit * 50).clear()
                    }
                }
            }
        }
    }
    
    private fun processBuffer() {
        // Buscar preámbulo [0, 7, 0, 7]
        if (buffer.size < samplesPerBit * 30) return
        
        val symbols = mutableListOf<Int>()
        for (i in 0 until minOf(buffer.size, samplesPerBit * 50) step samplesPerBit) {
            val chunk = buffer.subList(i, minOf(i + samplesPerBit, buffer.size))
            if (chunk.size < samplesPerBit) break
            symbols.add(detectSymbol(chunk))
        }
        
        // Buscar patrón de preámbulo
        for (i in 0 until symbols.size - 4) {
            if (symbols.subList(i, i + 4) == listOf(0, 7, 0, 7)) {
                val messageSymbols = symbols.subList(i + 4, minOf(i + 50, symbols.size))
                decodeMessage(messageSymbols)?.let { (sender, msg) ->
                    if (sender != username) {
                        onMessageReceived(sender, msg)
                    }
                }
                
                // Limpiar buffer
                val clearIndex = (i + 50) * samplesPerBit
                if (clearIndex < buffer.size) {
                    buffer.subList(0, clearIndex).clear()
                }
                return
            }
        }
    }
    
    private fun detectSymbol(chunk: List<Float>): Int {
        var maxEnergy = 0.0
        var detectedSymbol = 0
        
        for ((symbol, freq) in freqs.withIndex()) {
            val energy = goertzel(chunk, freq)
            if (energy > maxEnergy) {
                maxEnergy = energy
                detectedSymbol = symbol
            }
        }
        
        return detectedSymbol
    }
    
    private fun goertzel(samples: List<Float>, targetFreq: Int): Double {
        val k = (0.5 + samples.size * targetFreq / sampleRate).toInt()
        val omega = 2.0 * PI * k / samples.size
        val coeff = 2.0 * cos(omega)
        
        var q0 = 0.0
        var q1 = 0.0
        var q2 = 0.0
        
        for (sample in samples) {
            q0 = coeff * q1 - q2 + sample
            q2 = q1
            q1 = q0
        }
        
        val real = q1 - q2 * cos(omega)
        val imag = q2 * sin(omega)
        return real * real + imag * imag
    }
    
    private fun decodeMessage(symbols: List<Int>): Pair<String, String>? {
        // Convertir símbolos a bits (3 bits por símbolo)
        val bits = mutableListOf<Int>()
        for (symbol in symbols) {
            for (i in 2 downTo 0) {
                bits.add((symbol shr i) and 1)
            }
        }
        
        // Convertir bits a bytes
        val data = mutableListOf<Byte>()
        for (i in 0 until bits.size step 8) {
            if (i + 8 <= bits.size) {
                var byte = 0
                for (j in 0..7) {
                    byte = (byte shl 1) or bits[i + j]
                }
                data.add(byte.toByte())
            }
        }
        
        if (data.size < 2) return null
        
        val usernameLen = data[0].toInt() and 0xFF
        if (data.size < 1 + usernameLen) return null
        
        val sender = String(data.subList(1, 1 + usernameLen).toByteArray())
        val message = String(data.subList(1 + usernameLen, data.size).toByteArray()).trimEnd('\u0000')
        
        return Pair(sender, message)
    }
    
    fun sendMessage(message: String) {
        thread {
            val usernameBytes = username.toByteArray()
            val messageBytes = message.toByteArray()
            
            val data = mutableListOf<Byte>()
            data.add(usernameBytes.size.toByte())
            data.addAll(usernameBytes.toList())
            data.addAll(messageBytes.toList())
            
            // Generar audio
            val audio = mutableListOf<Short>()
            
            // Preámbulo
            for (symbol in listOf(0, 7, 0, 7)) {
                audio.addAll(generateTone(symbol))
            }
            
            // Convertir datos a bits
            val bits = mutableListOf<Int>()
            for (byte in data) {
                for (i in 7 downTo 0) {
                    bits.add((byte.toInt() shr i) and 1)
                }
            }
            
            // Convertir bits a símbolos (3 bits por símbolo)
            for (i in 0 until bits.size step 3) {
                var symbol = 0
                for (j in 0..2) {
                    if (i + j < bits.size) {
                        symbol = (symbol shl 1) or bits[i + j]
                    } else {
                        symbol = symbol shl 1
                    }
                }
                audio.addAll(generateTone(symbol))
            }
            
            // Enviar audio
            audioTrack?.write(audio.toShortArray(), 0, audio.size)
        }
    }
    
    private fun generateTone(symbol: Int): List<Short> {
        val freq = freqs[symbol]
        val samples = mutableListOf<Short>()
        
        for (i in 0 until samplesPerBit) {
            val t = i.toDouble() / sampleRate
            val value = sin(2 * PI * freq * t)
            samples.add((value * 32767 * 0.9).toInt().toShort())
        }
        
        return samples
    }
    
    fun stop() {
        isRunning = false
        audioRecord?.stop()
        audioRecord?.release()
        audioTrack?.stop()
        audioTrack?.release()
    }
}

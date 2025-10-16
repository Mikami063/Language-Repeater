package com.example.repeater

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var isRecording = false

    private lateinit var btnRecord: Button
    private lateinit var btnPlay: Button
    private lateinit var btnSave: Button
    private lateinit var statusText: TextView

    private lateinit var tempFile: File

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                status("Microphone permission denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnRecord = findViewById(R.id.btnRecord)
        btnPlay = findViewById(R.id.btnPlay)
        btnSave = findViewById(R.id.btnSave)
        statusText = findViewById(R.id.statusText)

        tempFile = File(cacheDir, "temp_recording.m4a")
        if (tempFile.exists()) tempFile.delete()

        btnRecord.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }
        btnPlay.setOnClickListener { onPlay() }
        btnSave.setOnClickListener { onSave() }

        ensureMicPermission()
    }

    private fun ensureMicPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startRecording() {
        stopPlaying()

        // auto-delete previous temp
        if (tempFile.exists()) {
            tempFile.delete()
        }

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    recorder?.apply {
                        setAudioSource(MediaRecorder.AudioSource.MIC)
                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                        setAudioEncodingBitRate(128000)
                        setAudioSamplingRate(44100)
                        setOutputFile(tempFile.absolutePath)
                        prepare()
                        start()
                    }
                }
                isRecording = true
                status("Recording…")
                btnRecord.text = "Stop"
                btnPlay.isEnabled = false
                btnSave.isEnabled = false
            } catch (e: IOException) {
                status("Recorder error: ${e.message}")
                recorder?.release()
                recorder = null
            }
        }
    }

    private fun stopRecording() {
        recorder?.apply {
            try {
                stop()
            } catch (e: Exception) {
                // ignore
            }
            release()
        }
        recorder = null
        isRecording = false
        status("Recording stopped.")
        btnRecord.text = "Record"
        btnPlay.isEnabled = true
        btnSave.isEnabled = true
    }

    private fun onPlay() {
        if (isRecording) {
            stopRecording()
        }
        if (!tempFile.exists()) {
            status("Nothing to play yet")
            return
        }

        stopPlaying()
        player = MediaPlayer()
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    player?.setDataSource(tempFile.absolutePath)
                    player?.prepare()
                }
                player?.start()
                player?.setOnCompletionListener {
                    stopPlaying()
                    status("Played")
                }
                status("Playing…")
            } catch (e: Exception) {
                status("Player error: ${e.message}")
                stopPlaying()
            }
        }
    }

    private fun stopPlaying() {
        player?.release()
        player = null
    }

    private fun onSave() {
        if (isRecording) {
            stopRecording()
        }
        if (!tempFile.exists()) {
            status("Nothing to save yet")
            return
        }

        val name = "repeater_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".m4a"
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, name)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Repeater")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }

        val resolver = contentResolver
        val collection: Uri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val itemUri = resolver.insert(collection, values)

        if (itemUri == null) {
            status("Save failed")
            return
        }

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(itemUri)?.use { out ->
                        tempFile.inputStream().use { it.copyTo(out) }
                    }
                    values.clear()
                    values.put(MediaStore.Audio.Media.IS_PENDING, 0)
                    resolver.update(itemUri, values, null, null)
                }
                status("Saved to Music/Repeater/$name")
            } catch (e: Exception) {
                status("Save error: ${e.message}")
            }
        }
    }

    private fun status(msg: String) {
        statusText.text = msg
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
        stopPlaying()
        if (tempFile.exists()) tempFile.delete()
    }
}

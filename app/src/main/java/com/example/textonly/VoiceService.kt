package text.only.app

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random
import java.io.IOException
import java.io.File
import kotlin.math.pow

class VoiceService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var mediaRecorder: MediaRecorder? = null
    private var audioJob: Job? = null
    
    private var isMuted = false
    
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val channelName = intent?.getStringExtra("CHANNEL_NAME")

        if (action == ACTION_START_VOICE) {
            if (channelName != null) {
                joinChannel(channelName)
            }
        } else if (action == ACTION_STOP_VOICE) {
            leaveChannel()
        } else if (action == "ACTION_MUTE") {
            isMuted = true
            stopAudioRecording()
        } else if (action == "ACTION_UNMUTE") {
            isMuted = false
            startAudioRecording()
        }

        return START_STICKY
    }

    private fun joinChannel(channelName: String) {
        VoiceConnectionManager.currentChannelName = channelName
        VoiceConnectionManager.isConnected = true
        VoiceConnectionManager.startSimulation(applicationContext, channelName) 

        startForegroundService()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startAudioRecording()
        }
    }

    private fun leaveChannel() {
        VoiceConnectionManager.currentChannelName = null
        VoiceConnectionManager.isConnected = false
        VoiceConnectionManager.stopSimulation()
        stopAudioRecording()
        
        stopForeground(true)
        stopSelf()
    }

    private fun startAudioRecording() {
        if (isMuted) return
        if (mediaRecorder != null) return
        
        try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                MediaRecorder()
            }
            
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            
            val tempFile = File(cacheDir, "voice_stream_temp.3gp")
            if (tempFile.exists()) {
                tempFile.delete()
            }
            recorder.setOutputFile(tempFile.absolutePath)
            
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
            
            startAmplitudeMonitoring()
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun stopAudioRecording() {
        audioJob?.cancel()
        audioJob = null
        
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) { }
        mediaRecorder = null
        
        val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val myName = prefs.getString("displayName", "Eu") ?: "Eu"
        
        val me = VoiceConnectionManager.users.find { it.name == myName }
        if (me != null && me.isSpeaking) {
            me.isSpeaking = false
            VoiceConnectionManager.notifyListeners()
        }
    }
    
    private fun startAmplitudeMonitoring() {
        audioJob = serviceScope.launch {
            delay(300) 

            while (isActive) {
                val prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                val myName = prefs.getString("displayName", "Eu") ?: "Eu"
                
                val voicePrefs = getSharedPreferences("VoiceSettings", Context.MODE_PRIVATE)
                val sensitivity = voicePrefs.getInt("mic_sensitivity", 50)

                val recorder = mediaRecorder
                if (recorder != null) {
                    val maxAmplitude = try { recorder.maxAmplitude } catch(e: Exception) { 0 }
                    
                    val factor = (100 - sensitivity) / 100.0
                    val threshold = 100 + (factor.pow(4) * 32000).toInt()

                    val isSpeakingNow = maxAmplitude > threshold
                    
                    val me = VoiceConnectionManager.users.find { it.name == myName }
                    if (me != null && me.isSpeaking != isSpeakingNow) {
                        me.isSpeaking = isSpeakingNow
                        VoiceConnectionManager.notifyListeners()
                    }
                }
                delay(100)
            }
        }
    }

    private fun startForegroundService() {
        val channelId = "VoiceChannel"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(channelId, "Voice Chat", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(chan)
        }
        
        val notificationIntent = Intent(this, ServerActivity::class.java) 
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, VoiceService::class.java).apply { action = ACTION_STOP_VOICE }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Conectat la voce")
            .setContentText("Canal: ${VoiceConnectionManager.currentChannelName}")
            .setSmallIcon(R.drawable.ic_headset) 
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Deconectare", stopPendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        stopAudioRecording()
        VoiceConnectionManager.stopSimulation()
    }

    companion object {
        const val ACTION_START_VOICE = "ACTION_START_VOICE"
        const val ACTION_STOP_VOICE = "ACTION_STOP_VOICE"
        
        fun start(context: Context, channelName: String) {
            val intent = Intent(context, VoiceService::class.java).apply {
                action = ACTION_START_VOICE
                putExtra("CHANNEL_NAME", channelName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, VoiceService::class.java).apply {
                action = ACTION_STOP_VOICE
            }
            context.startService(intent)
        }
    }
}

object VoiceConnectionManager {
    var currentChannelName: String? = null
    var isConnected: Boolean = false
    
    val listeners = CopyOnWriteArrayList<() -> Unit>()
    val users = CopyOnWriteArrayList<VoiceUser>()
    
    private var simulationJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    
    fun addUser(user: VoiceUser) {
        users.add(user)
        notifyListeners()
    }
    
    fun removeUser(userName: String) {
        users.removeIf { it.name == userName }
        notifyListeners()
    }
    
    fun notifyListeners() {
        listeners.forEach { it.invoke() }
    }
    
    fun startSimulation(context: Context, channelName: String) {
        if (simulationJob?.isActive == true) return
        
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val myName = prefs.getString("displayName", "Eu") ?: "Eu"
        
        users.removeIf { it.name != myName }
        
        // --- DEMO MODIFICARE: Eliminare Boți ---
        // Nu mai adăugăm boți deloc, lista rămâne doar cu utilizatorul curent.
        // when { ... } (eliminat)
        
        notifyListeners()
        
        simulationJob = scope.launch {
            while (isActive) {
                // Nu mai avem boți de animat, dar păstrăm bucla pentru viitor sau pentru a gestiona starea conexiunii
                // Doar asigurăm notificarea listenerilor în caz de schimbări externe
                delay(2000)
            }
        }
    }
    
    fun stopSimulation() {
        simulationJob?.cancel()
        users.clear()
        notifyListeners()
    }
}
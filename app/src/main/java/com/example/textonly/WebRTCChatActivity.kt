package com.example.textonly

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import org.webrtc.*
import java.nio.ByteBuffer

class WebRTCChatActivity : AppCompatActivity() {

    private val TAG = "WebRTCChat"
    private lateinit var localPeer: PeerConnection
    private lateinit var factory: PeerConnectionFactory
    private var dataChannel: DataChannel? = null
    private val db = FirebaseFirestore.getInstance()

    private lateinit var input: EditText
    private lateinit var output: TextView
    private lateinit var sendButton: Button

    private val roomId = "room1" // ⚠️ Folosește același ID pe ambele telefoane

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webrtc_chat)

        input = findViewById(R.id.inputMessage)
        output = findViewById(R.id.txtChat)
        sendButton = findViewById(R.id.btnSend)

        // 🔹 Inițializează WebRTC
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(this)
                .createInitializationOptions()
        )

        factory = PeerConnectionFactory.builder().createPeerConnectionFactory()

        // 🔹 Servere ICE (inclusiv TURN pentru conexiuni 4G ↔ WiFi)
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:relay1.expressturn.com:3478")
                .setUsername("ef5d6d")
                .setPassword("yjD8QvcP3Ff7sDnb")
                .createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers)

        // 🔹 Creează PeerConnection
        localPeer = factory.createPeerConnection(rtcConfig, object : PeerConnection.Observer {

            override fun onIceCandidate(candidate: IceCandidate) {
                Log.d(TAG, "ICE candidate: ${candidate.sdp}")
                db.collection("rooms").document(roomId)
                    .collection("candidates")
                    .add(
                        mapOf(
                            "sdpMid" to candidate.sdpMid,
                            "sdpMLineIndex" to candidate.sdpMLineIndex,
                            "sdp" to candidate.sdp
                        )
                    )
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {
                Log.d(TAG, "ICE candidates removed: ${candidates.size}")
            }

            override fun onDataChannel(dc: DataChannel) {
                dataChannel = dc
                dc.registerObserver(object : DataChannel.Observer {
                    override fun onMessage(buffer: DataChannel.Buffer) {
                        val bytes = ByteArray(buffer.data.remaining())
                        buffer.data.get(bytes)
                        runOnUiThread {
                            output.append("\n📩 ${String(bytes)}")
                        }
                    }

                    override fun onStateChange() {
                        Log.d(TAG, "DataChannel state: ${dc.state()}")
                    }

                    override fun onBufferedAmountChange(p0: Long) {}
                })
            }

            override fun onSignalingChange(newState: PeerConnection.SignalingState) {}

            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) {
                Log.d(TAG, "ICE connection state: $newState")
                runOnUiThread {
                    output.append("\n🌐 Conexiune: $newState")
                }
            }

            override fun onIceConnectionReceivingChange(p0: Boolean) {}
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) {}
            override fun onAddStream(p0: MediaStream) {}
            override fun onRemoveStream(p0: MediaStream) {}
            override fun onRenegotiationNeeded() {}
        })!!

        // 🔹 Creează canalul de date
        val init = DataChannel.Init()
        dataChannel = localPeer.createDataChannel("chat", init)
        dataChannel?.registerObserver(object : DataChannel.Observer {
            override fun onMessage(buffer: DataChannel.Buffer) {
                val bytes = ByteArray(buffer.data.remaining())
                buffer.data.get(bytes)
                runOnUiThread { output.append("\n📩 ${String(bytes)}") }
            }

            override fun onStateChange() {
                Log.d(TAG, "DataChannel state: ${dataChannel?.state()}")
            }

            override fun onBufferedAmountChange(l: Long) {}
        })

        // 🔹 Creează sau alătură-te camerei
        createOrJoinRoom()

        // 🔹 Trimiterea mesajelor
        sendButton.setOnClickListener {
            val text = input.text.toString()
            if (text.isNotEmpty()) {
                val buffer = ByteBuffer.wrap(text.toByteArray())
                dataChannel?.send(DataChannel.Buffer(buffer, false))
                output.append("\n📤 Tu: $text")
                input.text.clear()
            }
        }
    }

    private fun createOrJoinRoom() {
        val roomRef = db.collection("rooms").document(roomId)
        roomRef.get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                Log.d(TAG, "Creating offer...")
                localPeer.createOffer(object : SdpObserver {
                    override fun onCreateSuccess(desc: SessionDescription) {
                        localPeer.setLocalDescription(SimpleSdpObserver(), desc)
                        roomRef.set(mapOf("offer" to desc.description))
                        Log.d(TAG, "Offer created.")
                    }

                    override fun onSetSuccess() {}
                    override fun onCreateFailure(msg: String) {
                        Log.e(TAG, "Offer creation failed: $msg")
                    }

                    override fun onSetFailure(msg: String) {}
                }, MediaConstraints())
            } else {
                val offer = snapshot.getString("offer")!!
                Log.d(TAG, "Joining existing room.")
                localPeer.setRemoteDescription(
                    SimpleSdpObserver(),
                    SessionDescription(SessionDescription.Type.OFFER, offer)
                )

                localPeer.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(desc: SessionDescription) {
                        localPeer.setLocalDescription(SimpleSdpObserver(), desc)
                        roomRef.update("answer", desc.description)
                        Log.d(TAG, "Answer created.")
                    }

                    override fun onSetSuccess() {}
                    override fun onCreateFailure(msg: String) {
                        Log.e(TAG, "Answer creation failed: $msg")
                    }

                    override fun onSetFailure(msg: String) {}
                }, MediaConstraints())
            }
        }

        // 🔹 Ascultă răspunsul din Firestore
        roomRef.addSnapshotListener { snap, _ ->
            val answer = snap?.getString("answer")
            if (answer != null && localPeer.remoteDescription == null) {
                localPeer.setRemoteDescription(
                    SimpleSdpObserver(),
                    SessionDescription(SessionDescription.Type.ANSWER, answer)
                )
                Log.d(TAG, "Answer received.")
            }
        }

        // 🔹 Ascultă candidații ICE
        roomRef.collection("candidates").addSnapshotListener { snapshot, _ ->
            snapshot?.documentChanges?.forEach { change ->
                val data = change.document.data
                val candidate = IceCandidate(
                    data["sdpMid"] as String?,
                    (data["sdpMLineIndex"] as Long).toInt(),
                    data["sdp"] as String
                )
                localPeer.addIceCandidate(candidate)
                Log.d(TAG, "ICE candidate added: ${candidate.sdp}")
            }
        }
    }

    class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(p0: SessionDescription?) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(p0: String?) {}
        override fun onSetFailure(p0: String?) {}
    }
}

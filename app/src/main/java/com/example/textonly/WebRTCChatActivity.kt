package text.only.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class WebRTCChatActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Am eliminat referința la layout pentru a simplifica la maximum
        // setContentView(R.layout.activity_webrtc_chat) 

        /*
            TODO: Funcționalitatea WebRTC este temporar dezactivată.
            
            Pentru a o reactiva:
            1. Decomentează dependența WebRTC din fișierul build.gradle.kts.
            2. Rezolvă problema de descărcare a librăriei.
            3. Decomentează codul din acest fișier și din layout-ul corespunzător.
        */
    }
}

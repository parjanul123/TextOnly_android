package text.only.app

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.camera.view.PreviewView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

data class VoiceUser(
    val name: String, 
    val avatarRes: Int = R.mipmap.textonly_round,
    val avatarUri: String? = null,
    var isSpeaking: Boolean = false,
    var isCamOn: Boolean = false,
    var isMuted: Boolean = false,
    var isScreenShare: Boolean = false,
    var isBeingWatched: Boolean = false
)

class VoiceUserAdapter(
    private val users: List<VoiceUser>,
    private val myName: String,
    private val onBindCamera: (String, PreviewView) -> Unit,
    private val onFlipCamera: (String) -> Unit,
    private val onWatchStream: (String) -> Unit
) : RecyclerView.Adapter<VoiceUserAdapter.VoiceUserViewHolder>() {

    var isPiPMode: Boolean = false // Flag for PiP scaling

    class VoiceUserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: MaterialCardView = view.findViewById(R.id.cardVoiceUser)
        val imgAvatar: ImageView = view.findViewById(R.id.imgVoiceUserAvatar)
        val txtName: TextView = view.findViewById(R.id.txtVoiceUserName)
        val cameraPreview: PreviewView = view.findViewById(R.id.cameraPreview)
        val layoutAvatar: LinearLayout = view.findViewById(R.id.layoutAvatar)
        val imgMuteStatus: ImageView = view.findViewById(R.id.imgMuteStatus)
        val btnFlipCamera: ImageView = view.findViewById(R.id.btnFlipCamera)
        val btnWatchStream: Button = view.findViewById(R.id.btnWatchStream)
        val layoutScreenShareContent: View = view.findViewById(R.id.layoutScreenShareContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoiceUserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_voice_user, parent, false)
        return VoiceUserViewHolder(view)
    }

    override fun onBindViewHolder(holder: VoiceUserViewHolder, position: Int) {
        bindFull(holder, users[position])
    }

    override fun onBindViewHolder(holder: VoiceUserViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            val user = users[position]
            for (payload in payloads) {
                if (payload == "SPEAKING") {
                    updateSpeakingBorder(holder, user)
                } else if (payload == "MUTE") {
                    holder.imgMuteStatus.visibility = if (user.isMuted) View.VISIBLE else View.GONE
                } else if (payload == "WATCH_STATE") {
                    updateScreenShareState(holder, user)
                } else {
                    bindFull(holder, user)
                }
            }
        }
    }

    private fun bindFull(holder: VoiceUserViewHolder, user: VoiceUser) {
        // SCALING LOGIC FOR PIP
        val layoutParams = holder.card.layoutParams
        if (isPiPMode) {
            layoutParams.height = dpToPx(holder.itemView.context, 100) // Smaller height
            holder.txtName.textSize = 10f // Smaller text
            holder.imgAvatar.scaleX = 0.6f
            holder.imgAvatar.scaleY = 0.6f
        } else {
            layoutParams.height = dpToPx(holder.itemView.context, 180) // Normal height
            holder.txtName.textSize = 14f
            holder.imgAvatar.scaleX = 1f
            holder.imgAvatar.scaleY = 1f
        }
        holder.card.layoutParams = layoutParams

        holder.txtName.text = user.name
        
        // Screen Share Special UI
        if (user.isScreenShare) {
            holder.imgAvatar.setImageResource(R.drawable.ic_screen_share)
            holder.imgAvatar.imageTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
            
            holder.cameraPreview.visibility = View.GONE
            holder.btnFlipCamera.visibility = View.GONE
            holder.imgMuteStatus.visibility = View.GONE
            
            holder.card.strokeWidth = 6
            holder.card.strokeColor = 0xFF4CAF50.toInt()
            
            updateScreenShareState(holder, user)
            return
        }
        
        holder.btnWatchStream.visibility = View.GONE
        holder.layoutScreenShareContent.visibility = View.GONE
        holder.imgAvatar.imageTintList = null 
        
        if (user.avatarUri != null) {
             try {
                holder.imgAvatar.setImageURI(Uri.parse(user.avatarUri))
            } catch (e: Exception) {
                holder.imgAvatar.setImageResource(user.avatarRes)
            }
        } else {
            holder.imgAvatar.setImageResource(user.avatarRes)
        }

        updateSpeakingBorder(holder, user)

        holder.imgMuteStatus.visibility = if (user.isMuted) View.VISIBLE else View.GONE

        if (user.isCamOn) {
            holder.layoutAvatar.visibility = View.GONE
            holder.cameraPreview.visibility = View.VISIBLE
            onBindCamera(user.name, holder.cameraPreview)
            
            // Hide Flip button in PiP to save space
            if (user.name == myName && !isPiPMode) {
                holder.btnFlipCamera.visibility = View.VISIBLE
                holder.btnFlipCamera.setOnClickListener { onFlipCamera(user.name) }
            } else {
                holder.btnFlipCamera.visibility = View.GONE
            }
        } else {
            holder.layoutAvatar.visibility = View.VISIBLE
            holder.cameraPreview.visibility = View.GONE
            holder.btnFlipCamera.visibility = View.GONE
        }
    }
    
    private fun updateScreenShareState(holder: VoiceUserViewHolder, user: VoiceUser) {
        if (user.isBeingWatched) {
            holder.layoutScreenShareContent.visibility = View.VISIBLE
            holder.btnWatchStream.visibility = View.GONE
            holder.layoutAvatar.visibility = View.GONE
        } else {
            holder.layoutScreenShareContent.visibility = View.GONE
            // Hide button text or scale it in PiP?
            holder.btnWatchStream.visibility = View.VISIBLE
            if (isPiPMode) holder.btnWatchStream.textSize = 8f else holder.btnWatchStream.textSize = 12f
            
            holder.layoutAvatar.visibility = View.VISIBLE
            
            holder.btnWatchStream.setOnClickListener {
                onWatchStream(user.name)
            }
        }
    }
    
    private fun updateSpeakingBorder(holder: VoiceUserViewHolder, user: VoiceUser) {
        if (user.isScreenShare) return
        
        if (user.isSpeaking) {
            holder.card.strokeWidth = if (isPiPMode) 4 else 8
            holder.card.strokeColor = 0xFF2196F3.toInt() 
        } else {
            holder.card.strokeWidth = 0
        }
    }
    
    private fun dpToPx(context: android.content.Context, dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    override fun getItemCount() = users.size
}

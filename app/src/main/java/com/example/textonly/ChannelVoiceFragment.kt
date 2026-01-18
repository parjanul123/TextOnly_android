package text.only.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class ChannelVoiceFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_channel_voice, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val channelName = arguments?.getString("CHANNEL_NAME")
        view.findViewById<TextView>(R.id.txtVoiceChannelName).text = channelName
        
        // TODO: Aici vei adăuga logica de conectare/deconectare WebRTC
    }

    companion object {
        fun newInstance(channelName: String): ChannelVoiceFragment {
            val fragment = ChannelVoiceFragment()
            val args = Bundle()
            args.putString("CHANNEL_NAME", channelName)
            fragment.arguments = args
            return fragment
        }
    }
}

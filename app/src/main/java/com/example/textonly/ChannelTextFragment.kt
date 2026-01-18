package text.only.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

class ChannelTextFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflăm layout-ul pentru acest fragment
        return inflater.inflate(R.layout.fragment_channel_text, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Poți accesa argumentele (numele canalului) astfel:
        val channelName = arguments?.getString("CHANNEL_NAME")
        
        // TODO: Aici vei inițializa RecyclerView-ul și vei încărca mesajele pentru canalul `channelName`
    }

    companion object {
        // Metodă "fabrică" pentru a crea o instanță nouă a fragmentului cu argumente
        fun newInstance(channelName: String): ChannelTextFragment {
            val fragment = ChannelTextFragment()
            val args = Bundle()
            args.putString("CHANNEL_NAME", channelName)
            fragment.arguments = args
            return fragment
        }
    }
}

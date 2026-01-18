package text.only.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChannelTextFragment : Fragment() {

    private lateinit var recyclerMessages: RecyclerView
    private lateinit var inputMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var messageAdapter: MessageAdapter
    private val channelMessages = mutableListOf<ChatMessage>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_channel_text, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val channelName = arguments?.getString("CHANNEL_NAME")
        
        recyclerMessages = view.findViewById(R.id.recyclerChannelMessages)
        inputMessage = view.findViewById(R.id.inputChannelMessage)
        btnSend = view.findViewById(R.id.btnSendChannelMessage)
        
        // Setup RecyclerView
        messageAdapter = MessageAdapter(channelMessages, activity as MessageInteractionListener)
        recyclerMessages.layoutManager = LinearLayoutManager(context)
        recyclerMessages.adapter = messageAdapter
        
        // TODO: Aici vei încărca mesajele pentru canalul `channelName` de pe server
        
        btnSend.setOnClickListener {
            val messageText = inputMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                val textMessage = TextMessage(content = messageText, isSent = true)
                
                channelMessages.add(textMessage)
                messageAdapter.notifyItemInserted(channelMessages.size - 1)
                recyclerMessages.scrollToPosition(channelMessages.size - 1)
                
                inputMessage.text.clear()
                
                // TODO: Trimite `textMessage` pe canalul curent prin WebSocket
            }
        }
    }

    companion object {
        fun newInstance(channelName: String): ChannelTextFragment {
            val fragment = ChannelTextFragment()
            val args = Bundle()
            args.putString("CHANNEL_NAME", channelName)
            fragment.arguments = args
            return fragment
        }
    }
}

package text.only.app.qrlogin;

import android.util.Log;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.net.URISyntaxException;

public class MessagingWebSocketClient extends WebSocketClient {
    public interface MessageListener {
        void onMessageReceived(String messageJson);
    }
    private MessageListener listener;

    public MessagingWebSocketClient(String serverUri, MessageListener listener) throws URISyntaxException {
        super(new URI(serverUri));
        this.listener = listener;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.d("WebSocket", "Connected");
    }

    @Override
    public void onMessage(String message) {
        if (listener != null) listener.onMessageReceived(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.d("WebSocket", "Closed: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        Log.e("WebSocket", "Error: ", ex);
    }
}

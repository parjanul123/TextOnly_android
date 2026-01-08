package text.only.app.qrlogin;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.util.Log;
import ua.naiksoftware.stomp.Stomp;
import ua.naiksoftware.stomp.StompClient;
import io.reactivex.disposables.Disposable;
import io.reactivex.android.schedulers.AndroidSchedulers;

public class StompMessagingService {
    private StompClient stompClient;
    private Context context;
    private static final String CHANNEL_ID = "messages_channel";

    public StompMessagingService(Context context, String wsUrl) {
        this.context = context;
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl);
    }

    public void connectAndSubscribe() {
        stompClient.connect();
        Disposable disp = stompClient.topic("/topic/messages")
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(topicMessage -> {
                String payload = topicMessage.getPayload();
                Log.d("STOMP", "Mesaj primit: " + payload);
                showNotification("Mesaj nou", payload);
            }, throwable -> {
                Log.e("STOMP", "Eroare la primire mesaj", throwable);
            });
    }

    private void showNotification(String title, String content) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Mesaje", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}

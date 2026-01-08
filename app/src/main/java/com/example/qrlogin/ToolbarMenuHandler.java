package text.only.app.qrlogin;

import text.only.app.R;
import text.only.app.ScanQRActivity;
import android.app.Activity;
import android.content.Intent;
import android.widget.ImageButton;
import androidx.appcompat.widget.Toolbar;
import android.widget.PopupMenu;

public class ToolbarMenuHandler {
    public static void setupToolbar(Activity activity, Toolbar toolbar, ImageButton btnSettings) {
        btnSettings.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(activity, btnSettings);
            popup.getMenuInflater().inflate(R.menu.menu_toolbar, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.action_profile) {
                    // Deschide activitatea de profil
                    activity.startActivity(new Intent(activity, ProfileActivity.class));
                    return true;
                } else if (item.getItemId() == R.id.action_add_device) {
                    // Deschide activitatea de scanare QR
                    activity.startActivity(new Intent(activity, ScanQRActivity.class));
                    return true;
                }
                return false;
            });
            popup.show();
        });
    }
}

package text.only.app.qrlogin;

import text.only.app.BuyOnlycoinsActivity;
import text.only.app.Config;
import text.only.app.R;
import text.only.app.WalletActivity;
import text.only.app.StoreActivity;
import text.only.app.InventoryActivity;
import text.only.app.TransactionsActivity;
import text.only.app.AnimatedFrameView; // Import Custom View
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileActivity extends Activity {
    private EditText editDisplayName;
    private Button btnSave;
    private ImageView imgProfile;
    private ImageView imgAvatarFrame;
    private AnimatedFrameView viewAnimatedFrame; // Reference to custom view
    private Button btnWallet;
    private Button btnStore;
    private Button btnInventory;
    private Button btnTransactions;

    private static final int PICK_IMAGE = 100;

    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_DISPLAY_NAME = "displayName";
    private static final String KEY_PROFILE_IMAGE = "profileImageUri";
    private static final String KEY_EQUIPPED_FRAME = "equipped_frame";

    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        editDisplayName = findViewById(R.id.edit_display_name);
        btnSave = findViewById(R.id.btn_save);
        imgProfile = findViewById(R.id.img_profile);
        imgAvatarFrame = findViewById(R.id.img_avatar_frame);
        viewAnimatedFrame = findViewById(R.id.view_animated_frame);
        btnWallet = findViewById(R.id.btn_wallet);
        btnStore = findViewById(R.id.btn_store);
        btnInventory = findViewById(R.id.btn_inventory);
        btnTransactions = findViewById(R.id.btn_transactions);

        loadProfileData();

        imgProfile.setOnClickListener(v -> openGallery());
        btnSave.setOnClickListener(v -> saveProfileData());
        btnWallet.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, WalletActivity.class);
            startActivity(intent);
        });
        
        btnStore.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, StoreActivity.class)));
        btnInventory.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, InventoryActivity.class)));
        btnTransactions.setOnClickListener(v -> startActivity(new Intent(ProfileActivity.this, TransactionsActivity.class)));
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadProfileData(); 
    }

    private void openGallery() {
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        // ✅ Cerem permisiuni
        gallery.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(gallery, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                try {
                    // ✅ Încercăm să luăm permisiuni persistente (poate eșua pe unele versiuni, dar e ok)
                    final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                    getContentResolver().takePersistableUriPermission(imageUri, takeFlags);
                } catch (Exception e) {
                    Log.e("ProfileActivity", "Nu am putut lua permisiuni persistente: " + e.getMessage());
                }

                // Setăm imaginea
                try {
                    imgProfile.setImageURI(imageUri);
                    // Salvăm doar dacă setarea a reușit
                    SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    prefs.edit().putString(KEY_PROFILE_IMAGE, imageUri.toString()).apply();
                } catch (Exception e) {
                    Toast.makeText(this, "Nu am putut încărca imaginea.", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void loadProfileData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String name = prefs.getString(KEY_DISPLAY_NAME, "");
        String imageUriString = prefs.getString(KEY_PROFILE_IMAGE, null);
        String equippedFrame = prefs.getString(KEY_EQUIPPED_FRAME, null);

        editDisplayName.setText(name);

        if (imageUriString != null) {
            try {
                Uri imageUri = Uri.parse(imageUriString);
                imgProfile.setImageURI(imageUri);
            } catch (Exception e) {
                Log.e("ProfileActivity", "Eroare la încărcarea imaginii salvate. Resetez.", e);
                prefs.edit().remove(KEY_PROFILE_IMAGE).apply();
                imgProfile.setImageResource(R.mipmap.ic_launcher_round); 
            }
        }
        
        // Load Frame Logic
        imgAvatarFrame.setVisibility(View.GONE);
        viewAnimatedFrame.setVisibility(View.GONE);

        if (equippedFrame != null) {
            if (equippedFrame.equals("ic_frame_rain")) {
                // Use Animated View for Rain
                viewAnimatedFrame.setVisibility(View.VISIBLE);
                viewAnimatedFrame.setFrameType("frame_rain");
            } else if (equippedFrame.equals("ic_coin_shape")) {
                // Use Static View for simple neon
                imgAvatarFrame.setVisibility(View.VISIBLE);
                imgAvatarFrame.setImageResource(R.drawable.ic_coin_shape);
            }
        }
    }

    private void saveProfileData() {
        String name = editDisplayName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Te rog completează numele de utilizator", Toast.LENGTH_SHORT).show();
            return;
        }
        sendProfileToBackend(name);
    }

    private void sendProfileToBackend(String name) {
        MediaType JSON = MediaType.get("application/json; charset=utf-8");
        JSONObject json = new JSONObject();
        try {
            json.put("displayName", name);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(json.toString(), JSON);
        Request request = new Request.Builder()
                .url(Config.PROFILE_UPDATE_URL)
                .post(body)
                .build();

        btnSave.setEnabled(false);
        btnSave.setText("Se salvează...");

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this, "Eroare conexiune server! Verifică Ngrok.", Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                    btnSave.setText("Salvează Profil");
                    Log.e("ProfileActivity", "Error updating profile", e);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    if (response.isSuccessful()) {
                        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        prefs.edit().putString(KEY_DISPLAY_NAME, name).apply();

                        Toast.makeText(ProfileActivity.this, "Profil actualizat! ✅", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(ProfileActivity.this, "Eroare server: " + response.code(), Toast.LENGTH_SHORT).show();
                        btnSave.setEnabled(true);
                        btnSave.setText("Salvează Profil");
                    }
                });
            }
        });
    }
}

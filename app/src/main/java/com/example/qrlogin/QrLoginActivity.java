package com.example.qrlogin;

import com.example.textonly.Config;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class QrLoginActivity extends Activity {
    private String phoneNumber = ""; 

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new IntentIntegrator(this).initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if(result != null && result.getContents() != null) {
            String token = result.getContents();
            sendTokenToBackend(token, phoneNumber);
        } else {
            Toast.makeText(this, "Scanare anulată", Toast.LENGTH_SHORT).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void sendTokenToBackend(String token, String phoneNumber) {
        new Thread(() -> {
            try {
                // Folosim URL-ul din Config (Ngrok)
                URL url = new URL(Config.QR_VALIDATE_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                JSONObject body = new JSONObject();
                body.put("token", token);
                body.put("phoneNumber", phoneNumber);
                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes());
                os.flush();
                os.close();
                int responseCode = conn.getResponseCode();
                runOnUiThread(() -> {
                    if(responseCode == 200) {
                        Toast.makeText(this, "Login pe web reușit!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Eroare la login web", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Eroare: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}

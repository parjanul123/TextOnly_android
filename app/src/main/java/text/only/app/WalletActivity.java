package text.only.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class WalletActivity extends AppCompatActivity {

    private TextView txtCoinBalance;
    private Button btnBuyCoins;
    private Button btnSellCoins;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.Theme_TextOnly);
        setContentView(R.layout.activity_wallet);

        txtCoinBalance = findViewById(R.id.txt_coin_balance);
        btnBuyCoins = findViewById(R.id.btn_buy_coins);
        btnSellCoins = findViewById(R.id.btn_sell_coins);

        // 🔹 DESCHIDE BUY
        btnBuyCoins.setOnClickListener(v -> {
            Intent intent = new Intent(
                    WalletActivity.this,
                    BuyOnlycoinsActivity.class
            );
            startActivity(intent);
        });

        // 🔹 DESCHIDE SELL
        btnSellCoins.setOnClickListener(v -> {
            Intent intent = new Intent(
                    WalletActivity.this,
                    SellOnlycoinsActivity.class
            );
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Load real balance from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        
        // --- DEMO START: Set wallet to 0 coins (RESET) ---
        prefs.edit().putInt("userCoins", 0).apply();
        // --- DEMO END ---

        int userCoins = prefs.getInt("userCoins", 0); 
        
        // Textul conține doar numărul
        txtCoinBalance.setText(String.valueOf(userCoins));
    }
}

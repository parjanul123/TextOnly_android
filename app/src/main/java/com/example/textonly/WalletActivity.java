package text.only.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class WalletActivity extends AppCompatActivity {

    private TextView txtCoinBalance;
    private Button btnBuyCoins;
    private Button btnSellCoins;

    public static final String PREFS_NAME = "WalletPrefs";
    public static final String KEY_COIN_BALANCE = "coin_balance";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Folosim o temă AppCompat pentru compatibilitate maximă
        setTheme(R.style.Theme_TextOnly);
        setContentView(R.layout.activity_wallet);

        txtCoinBalance = findViewById(R.id.txt_coin_balance);
        btnBuyCoins = findViewById(R.id.btn_buy_coins);
        btnSellCoins = findViewById(R.id.btn_sell_coins);

        btnBuyCoins.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WalletActivity.this, BuyOnlycoinsActivity.class));
            }
        });

        btnSellCoins.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WalletActivity.this, SellOnlycoinsActivity.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateBalance();
    }

    private void updateBalance() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int balance = prefs.getInt(KEY_COIN_BALANCE, 0);
        txtCoinBalance.setText(String.format("%d OnlyCoins", balance));
    }
}

package text.only.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.*

class SellOnlycoinsActivity : Activity() {

    private lateinit var editSellAmount: EditText
    private lateinit var btnConfirmSell: Button
    private lateinit var spinnerCurrencySell: Spinner
    private lateinit var txtSellPreview: TextView

    // 🔹 CONSTANTE WALLET (nu mai depindem de WalletActivity)
    private val PREFS_NAME = "WalletPrefs"
    private val KEY_COIN_BALANCE = "coin_balance"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sell_onlycoins)

        editSellAmount = findViewById(R.id.edit_sell_amount)
        btnConfirmSell = findViewById(R.id.btn_confirm_sell)
        spinnerCurrencySell = findViewById(R.id.spinnerCurrencySell)
        txtSellPreview = findViewById(R.id.txtSellPreview)

        setupSpinner()

        editSellAmount.addTextChangedListener(simpleWatcher)

        spinnerCurrencySell.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    updateSellPreview()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        btnConfirmSell.setOnClickListener {
            onSellClicked()
        }
    }

    // 🔹 Spinner cu text verde
    private fun setupSpinner() {
        val adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            resources.getStringArray(R.array.currencies)
        ) {
            override fun getView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).setTextColor(Color.parseColor("#00E676"))
                return view
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCurrencySell.adapter = adapter
    }

    private val simpleWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            updateSellPreview()
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    private fun updateSellPreview() {
        val coins = editSellAmount.text.toString().toIntOrNull() ?: 0
        val currency = spinnerCurrencySell.selectedItem.toString()

        val value = if (currency == "EUR") coins / 10.0 else coins / 2.0
        txtSellPreview.text = String.format("Vei primi: %.2f %s", value, currency)
    }

    // 🔹 REDIRECȚIONARE CĂTRE PAGINA IBAN (backend)
    private fun onSellClicked() {
        val amountStr = editSellAmount.text.toString()
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Introdu suma de vândut", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toInt()

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val currentBalance = prefs.getInt(KEY_COIN_BALANCE, 0)

        if (currentBalance < amount) {
            Toast.makeText(this, "Fonduri insuficiente!", Toast.LENGTH_SHORT).show()
            return
        }

        val currency = spinnerCurrencySell.selectedItem.toString()

        val backendUrl =
            "https://textonly-backend.onrender.com/iban.html?coins=$amount&currency=$currency"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(backendUrl))
        startActivity(intent)
    }
}

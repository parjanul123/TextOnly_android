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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sell_onlycoins)

        // 🔹 findViewById
        editSellAmount = findViewById(R.id.edit_sell_amount)
        btnConfirmSell = findViewById(R.id.btn_confirm_sell)
        spinnerCurrencySell = findViewById(R.id.spinnerCurrencySell)
        txtSellPreview = findViewById(R.id.txtSellPreview)

        // 🔹 Spinner cu text verde
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

        // 🔹 update preview când scrii
        editSellAmount.addTextChangedListener(simpleWatcher)

        // 🔹 update preview când schimbi valuta
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

        // 🔹 BUTON VINDE
        btnConfirmSell.setOnClickListener {
            onSellClicked()
        }
    }

    // 🔹 TextWatcher simplu
    private val simpleWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            updateSellPreview()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    // 🔹 Preview sumă primită
    private fun updateSellPreview() {
        val coins = editSellAmount.text.toString().toIntOrNull() ?: 0
        val currency = spinnerCurrencySell.selectedItem.toString()

        val value = if (currency == "EUR") {
            coins / 10.0
        } else {
            coins / 2.0 // RON
        }

        txtSellPreview.text = String.format("Vei primi: %.2f %s", value, currency)
    }

    // 🔹 Logică vânzare (doar redirecționare)
    private fun onSellClicked() {
        val amountStr = editSellAmount.text.toString()
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Introdu suma de vândut", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toInt()

        val prefs = getSharedPreferences(WalletActivity.PREFS_NAME, MODE_PRIVATE)
        val currentBalance = prefs.getInt(WalletActivity.KEY_COIN_BALANCE, 0)

        if (currentBalance < amount) {
            Toast.makeText(this, "Fonduri insuficiente!", Toast.LENGTH_SHORT).show()
            return
        }

        // 🔹 URL către pagina HTML din backend (Render)
        val backendUrl = "https://TEXTONLY-BACKEND.onrender.com/iban.html?coins=$amount"

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(backendUrl))
        startActivity(intent)
    }
}

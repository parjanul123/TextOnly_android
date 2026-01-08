package com.example.textonly

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class BuyOnlycoinsActivity : AppCompatActivity() {

    private lateinit var spinnerCurrency: Spinner
    private lateinit var editAmount: EditText
    private lateinit var txtCoinsPreview: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_TextOnly)
        setContentView(R.layout.activity_buy_onlycoins)

        // 🔹 findViewById
        spinnerCurrency = findViewById(R.id.spinnerCurrency)
        editAmount = findViewById(R.id.editAmount)
        txtCoinsPreview = findViewById(R.id.txtCoinsPreview)
        val btnChooseMethod = findViewById<Button>(R.id.btnChooseMethod)

        // 🔹 ADAPTER – text RON / EUR VERDE în chenar
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
                val textView = view as TextView
                textView.setTextColor(android.graphics.Color.parseColor("#00E676"))
                return view
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCurrency.adapter = adapter

        // 🔹 update când se schimbă suma
        editAmount.addTextChangedListener(simpleWatcher)

        // 🔹 update când se schimbă valuta
        spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                updateCoinsPreview()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // 🔹 buton
        btnChooseMethod.setOnClickListener {
            val amount = editAmount.text.toString().toDoubleOrNull()
            if (amount == null || amount < 5) {
                Toast.makeText(this, "Introdu o sumă minimă de 5", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showPaymentDialog(amount)
        }
    }

    private val simpleWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            updateCoinsPreview()
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    private fun updateCoinsPreview() {
        val amount = editAmount.text.toString().toDoubleOrNull() ?: 0.0
        val currency = spinnerCurrency.selectedItem.toString()

        val rate = if (currency == "EUR") 10 else 2
        val coins = (amount * rate).toInt()

        txtCoinsPreview.text = "Vei primi: $coins OnlyCoins"
    }

    private fun showPaymentDialog(amount: Double) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_payment_method)

        dialog.findViewById<ImageView>(R.id.imgVisa).setOnClickListener {
            simulatePayment("Visa / Mastercard", amount)
            dialog.dismiss()
        }

        dialog.findViewById<ImageView>(R.id.imgPaysafe).setOnClickListener {
            simulatePayment("Paysafecard", amount)
            dialog.dismiss()
        }

        dialog.findViewById<ImageView>(R.id.imgCoinbase).setOnClickListener {
            simulatePayment("Coinbase (+15% bonus)", amount * 1.15)
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun simulatePayment(method: String, amount: Double) {
        updateCoinsPreview()
        Toast.makeText(
            this,
            "Plată efectuată cu $method ✅\n${txtCoinsPreview.text}",
            Toast.LENGTH_LONG
        ).show()
    }
}

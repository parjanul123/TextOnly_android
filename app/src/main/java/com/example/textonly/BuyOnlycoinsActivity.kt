package com.example.textonly

import android.app.Dialog
import android.os.Bundle
import android.widget.*
import androidx.activity.ComponentActivity

class BuyOnlycoinsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buy_onlycoins)

        val spinnerCurrency = findViewById<Spinner>(R.id.spinnerCurrency)
        val editAmount = findViewById<EditText>(R.id.editAmount)
        val btnChooseMethod = findViewById<Button>(R.id.btnChooseMethod)

        btnChooseMethod.setOnClickListener {
            val amount = editAmount.text.toString().toDoubleOrNull()
            if (amount == null || amount < 5) {
                Toast.makeText(this, "Introdu o sumă minimă de 5 RON", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showPaymentDialog(amount)
        }
    }

    private fun showPaymentDialog(amount: Double) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_payment_method)

        val imgVisa = dialog.findViewById<ImageView>(R.id.imgVisa)
        val imgPaysafe = dialog.findViewById<ImageView>(R.id.imgPaysafe)
        val imgCoinbase = dialog.findViewById<ImageView>(R.id.imgCoinbase)
        val btnClose = dialog.findViewById<Button>(R.id.btnClose)

        imgVisa.setOnClickListener {
            simulatePayment("Visa / Mastercard", amount)
            dialog.dismiss()
        }

        imgPaysafe.setOnClickListener {
            simulatePayment("Paysafecard", amount)
            dialog.dismiss()
        }

        imgCoinbase.setOnClickListener {
            simulatePayment("Coinbase (+15% bonus)", amount * 1.15)
            dialog.dismiss()
        }

        btnClose.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    private fun simulatePayment(method: String, amount: Double) {
        val coins = (amount * 2).toInt() // 1 leu = 2 OnlyCoins
        Toast.makeText(
            this,
            "Plată efectuată cu $method ✅\nAi primit $coins OnlyCoins",
            Toast.LENGTH_LONG
        ).show()
    }
}

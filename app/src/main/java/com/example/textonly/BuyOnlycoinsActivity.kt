package text.only.app

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*
import android.graphics.Color

class BuyOnlycoinsActivity : AppCompatActivity() {

    private lateinit var spinnerCurrency: Spinner
    private lateinit var editAmount: EditText
    private lateinit var txtCoinsPreview: TextView
    private lateinit var btnBuy: Button

    private lateinit var billingClient: BillingClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_TextOnly)
        setContentView(R.layout.activity_buy_onlycoins)

        // 🔹 UI
        spinnerCurrency = findViewById(R.id.spinnerCurrency)
        editAmount = findViewById(R.id.editAmount)
        txtCoinsPreview = findViewById(R.id.txtCoinsPreview)
        btnBuy = findViewById(R.id.btnChooseMethod)

        // 🔹 Spinner cu text verde
        val adapter = object : ArrayAdapter<String>(
            this,
            android.R.layout.simple_spinner_item,
            resources.getStringArray(R.array.currencies)
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                (view as TextView).setTextColor(Color.parseColor("#00E676"))
                return view
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCurrency.adapter = adapter

        // 🔹 listeners preview
        editAmount.addTextChangedListener(simpleWatcher)
        spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateCoinsPreview()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // 🔹 Google Billing init
        initBilling()

        // 🔹 BUY
        btnBuy.setOnClickListener {
            val amount = editAmount.text.toString().toIntOrNull()
            if (amount == null || amount < 5) {
                Toast.makeText(this, "Introdu o sumă minimă", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // alegi produsul (exemplu)
            val productId = when {
                amount >= 1000 -> "onlycoins_1000"
                amount >= 500 -> "onlycoins_500"
                else -> "onlycoins_100"
            }

            startGooglePurchase(productId)
        }
    }

    // 🔹 PREVIEW COINS
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

    // ===============================
    // 🔥 GOOGLE PLAY BILLING
    // ===============================

    private fun initBilling() {
        billingClient = BillingClient.newBuilder(this)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {}
            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun startGooglePurchase(productId: String) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { _, productDetailsList ->
            val productDetails = productDetailsList.firstOrNull() ?: return@queryProductDetailsAsync

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .build()
                    )
                )
                .build()

            billingClient.launchBillingFlow(this, billingFlowParams)
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        Toast.makeText(
            this,
            "Plată confirmată ✅\nOnlyCoins adăugați",
            Toast.LENGTH_LONG
        ).show()

        // 🔐 PASUL URMĂTOR (OBLIGATORIU):
        // trimite purchase.purchaseToken la backend (Spring Boot)
        // backend validează cu Google Play Developer API
    }
}

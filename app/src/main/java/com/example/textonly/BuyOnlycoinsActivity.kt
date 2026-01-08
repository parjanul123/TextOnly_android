package text.only.app

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*

class BuyOnlycoinsActivity : AppCompatActivity(), PurchasesUpdatedListener {

    private lateinit var spinnerCurrency: Spinner
    private lateinit var editAmount: EditText
    private lateinit var txtCoinsPreview: TextView
    private lateinit var btnBuy: Button

    private lateinit var billingClient: BillingClient
    private var selectedProductId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_TextOnly)
        setContentView(R.layout.activity_buy_onlycoins)

        spinnerCurrency = findViewById(R.id.spinnerCurrency)
        editAmount = findViewById(R.id.editAmount)
        txtCoinsPreview = findViewById(R.id.txtCoinsPreview)
        btnBuy = findViewById(R.id.btnChooseMethod)

        setupSpinner()
        setupBilling()

        editAmount.addTextChangedListener(simpleWatcher)

        spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                updateCoinsPreview()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnBuy.setOnClickListener {
            startGoogleBilling()
        }
    }

    // 🔹 Spinner
    private fun setupSpinner() {
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
    }

    // 🔹 TextWatcher
    private val simpleWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            updateCoinsPreview()
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    // 🔹 Preview coins
    private fun updateCoinsPreview() {
        val amount = editAmount.text.toString().toDoubleOrNull() ?: 0.0
        val currency = spinnerCurrency.selectedItem.toString()
        val rate = if (currency == "EUR") 10 else 2
        val coins = (amount * rate).toInt()

        txtCoinsPreview.text = "Vei primi: $coins OnlyCoins"

        // mapăm suma pe produse reale din Play Console
        selectedProductId = when {
            coins >= 200 -> "onlycoins_200"
            coins >= 100 -> "onlycoins_100"
            else -> "onlycoins_50"
        }
    }

    // 🔹 Google Billing setup
    private fun setupBilling() {
        billingClient = BillingClient.newBuilder(this)
            .setListener(this)
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    Toast.makeText(
                        this@BuyOnlycoinsActivity,
                        "Eroare Google Billing",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onBillingServiceDisconnected() {}
        })
    }

    // 🔹 Pornește plata Google
    private fun startGoogleBilling() {
        val amount = editAmount.text.toString().toDoubleOrNull()
        if (amount == null || amount < 1) {
            Toast.makeText(this, "Introdu o sumă minimă de 1", Toast.LENGTH_SHORT).show()
            return
        }

        val query = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(selectedProductId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(query) { _, productDetailsList ->
            if (productDetailsList.isEmpty()) {
                Toast.makeText(this, "Produs indisponibil", Toast.LENGTH_SHORT).show()
                return@queryProductDetailsAsync
            }

            val productDetails = productDetailsList[0]

            val billingParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .build()
                    )
                )
                .build()

            billingClient.launchBillingFlow(this, billingParams)
        }
    }

    // 🔹 Rezultat plată
    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            Toast.makeText(this, "Plată reușită ✅ OnlyCoins adăugate", Toast.LENGTH_LONG).show()
            // aici poți adăuga coins în Wallet / backend
        }
    }
}

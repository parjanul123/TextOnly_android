package text.only.app


import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class GiftBottomSheet(
    private val userCoins: Int,
    private val onGiftSelected: (Gift) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var recyclerGifts: RecyclerView
    private lateinit var adapter: GiftAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.bottomsheet_gifts, container, false)
        recyclerGifts = view.findViewById(R.id.recyclerGifts)

        val giftList = listOf(
            Gift("❤️ Inimă", R.drawable.ic_heart, 10),
            Gift("🌹 Trandafir", R.drawable.ic_flower, 20),
            Gift("🎉 Confetti", R.drawable.ic_confetti, 30),
            Gift("👑 Coroană", R.drawable.ic_crown, 50),
            Gift("🚀 Rachetă", R.drawable.ic_rocket, 100)
        ).sortedBy { it.price }

        adapter = GiftAdapter(giftList) { gift ->
            if (userCoins >= gift.price) {
                onGiftSelected(gift)
                dismiss()
            } else {
                Toast.makeText(context, "Nu ai destui OnlyCoins 💸", Toast.LENGTH_SHORT).show()
            }
        }

        recyclerGifts.layoutManager = GridLayoutManager(context, 3)
        recyclerGifts.adapter = adapter

        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setDimAmount(0.3f) // fundal semi-transparent
        return dialog
    }
}

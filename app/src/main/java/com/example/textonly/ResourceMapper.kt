package text.only.app

import android.util.Log

object ResourceMapper {
    fun getDrawableId(name: String?): Int {
        if (name == null) return R.drawable.ic_coin_shape // Fallback default

        return try {
            // Mapping explicit
            when (name) {
                "ic_rose" -> R.drawable.ic_rose
                "ic_frame_rain" -> R.drawable.ic_frame_rain
                "ic_heart" -> R.drawable.ic_heart
                "ic_rocket" -> R.drawable.ic_rocket
                "ic_gift_card" -> R.drawable.ic_gift_card
                "emote_happy" -> R.drawable.emote_happy 
                "emote_cat" -> R.drawable.emote_cat
                "emote_sad" -> R.drawable.emote_sad
                "ic_coin_shape" -> R.drawable.ic_coin_shape
                else -> {
                    // Fallback generic dacă numele nu e recunoscut
                    Log.w("ResourceMapper", "Resource not found for name: $name, using default.")
                    R.drawable.ic_coin_shape 
                }
            }
        } catch (e: Exception) {
            Log.e("ResourceMapper", "Error mapping resource: $name", e)
            R.drawable.ic_coin_shape
        }
    }
}

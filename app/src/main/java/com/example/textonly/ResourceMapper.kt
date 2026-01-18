package text.only.app

import text.only.app.R

object ResourceMapper {
    fun getDrawableId(resourceName: String?): Int {
        return when (resourceName) {
            "ic_rose" -> R.drawable.ic_rose
            "ic_frame_rain" -> R.drawable.ic_frame_rain
            "ic_heart" -> R.drawable.ic_heart
            "ic_rocket" -> R.drawable.ic_rocket
            "ic_gift_card" -> R.drawable.ic_gift_card
            "frame_fire" -> R.drawable.ic_coin_shape // Placeholder if missing
            "emote_happy" -> R.drawable.ic_coin_shape // Placeholder
            "emote_cat" -> R.drawable.ic_coin_shape // Placeholder
            "emote_sad" -> R.drawable.ic_coin_shape // Placeholder
            else -> R.drawable.ic_coin_shape
        }
    }
}

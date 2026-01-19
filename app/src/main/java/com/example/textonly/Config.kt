package text.only.app

object Config {
    // ✅ URL-ul actualizat din screenshot-ul Ngrok
    const val BASE_DOMAIN = "ungrindable-mellissa-participially.ngrok-free.dev" 
    
    // URL-uri derivate
    const val BASE_URL = "https://$BASE_DOMAIN"
    const val WS_URL = "wss://$BASE_DOMAIN/ws" // WebSocket pentru Chat
    
    // Endpoint-uri (Am scos prefixul '/api' pentru a rezolva eroarea 404)
    const val QR_VALIDATE_URL = "$BASE_URL/auth/qr/validate"
    const val PROFILE_UPDATE_URL = "$BASE_URL/users/profile"
    const val CHAT_GPT_URL = "$BASE_URL/ai/chat"
    
    // Store & Inventory (Backend Endpoints)
    const val STORE_ITEMS_URL = "$BASE_URL/store/items"
    const val BUY_ITEM_URL = "$BASE_URL/store/buy"
    const val INVENTORY_URL = "$BASE_URL/store/inventory"
    
    // Actualizări aplicație
    const val APP_UPDATE_URL = "$BASE_URL/app/version"
}
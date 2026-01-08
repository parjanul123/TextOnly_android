package text.only.app

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ChatService {

    private val client = OkHttpClient()

    fun sendMessageToAI(message: String, callback: (String?) -> Unit) {
        val json = JSONObject()
        json.put("message", message)
        
        // Puteți adăuga și alte câmpuri dacă backend-ul o cere (ex: userId, sessionId)
        // json.put("userId", "...")

        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        
        val request = Request.Builder()
            .url(Config.CHAT_GPT_URL)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ChatService", "AI Request Failed", e)
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    try {
                        // Presupunem că backend-ul returnează: { "reply": "Hello from ChatGPT..." }
                        val jsonResponse = JSONObject(responseBody ?: "{}")
                        val reply = jsonResponse.optString("reply", "No response content")
                        callback(reply)
                    } catch (e: Exception) {
                        Log.e("ChatService", "JSON Parse Error", e)
                        callback("Error parsing AI response")
                    }
                } else {
                    Log.e("ChatService", "Server Error: ${response.code}")
                    callback("Server Error: ${response.code}")
                }
            }
        })
    }
}

package com.example.textonly

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import okhttp3.*
import org.json.JSONObject
import java.io.File
import java.io.IOException

class UpdateChecker(private val context: Context) {

    private val client = OkHttpClient()

    // 🔴 Trebuie să implementezi acest endpoint pe backend
    // Ar trebui să returneze JSON: { "versionCode": 2, "url": "..." }
    private val UPDATE_URL = "${Config.BASE_URL}/api/app/version"

    fun checkForUpdates() {
        val request = Request.Builder().url(UPDATE_URL).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("UpdateChecker", "Failed to check for updates", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        try {
                            val json = JSONObject(body)
                            val serverVersionCode = json.optInt("versionCode", -1)
                            val downloadUrl = json.optString("url", "")

                            // Obține versiunea curentă a aplicației
                            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                            val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                pInfo.longVersionCode.toInt()
                            } else {
                                @Suppress("DEPRECATION")
                                pInfo.versionCode
                            }

                            if (serverVersionCode > currentVersionCode && downloadUrl.isNotEmpty()) {
                                showUpdateDialog(downloadUrl)
                            } else {
                                Log.d("UpdateChecker", "App is up to date.")
                            }

                        } catch (e: Exception) {
                            Log.e("UpdateChecker", "JSON Parse Error", e)
                        }
                    }
                }
            }
        })
    }

    private fun showUpdateDialog(downloadUrl: String) {
        // Trebuie să rulăm pe UI Thread pentru a afișa dialogul
        if (context is android.app.Activity) {
            context.runOnUiThread {
                AlertDialog.Builder(context)
                    .setTitle("Actualizare Disponibilă")
                    .setMessage("O nouă versiune a aplicației este disponibilă. Vrei să o descarci acum?")
                    .setPositiveButton("Actualizează") { _, _ ->
                        downloadAndInstallApk(downloadUrl)
                    }
                    .setNegativeButton("Mai târziu", null)
                    .show()
            }
        }
    }

    private fun downloadAndInstallApk(url: String) {
        val destination = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val fileName = "update.apk"
        val file = File(destination, fileName)
        
        // Șterge fișierul vechi dacă există
        if (file.exists()) {
            file.delete()
        }

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Descărcare Actualizare")
            .setDescription("Se descarcă noua versiune...")
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)

        Toast.makeText(context, "Descărcare începută...", Toast.LENGTH_SHORT).show()

        // Ascultă finalizarea descărcării
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    installApk(file)
                    context.unregisterReceiver(this)
                }
            }
        }
        context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
    }

    private fun installApk(file: File) {
        try {
            if (!file.exists()) {
                Log.e("UpdateChecker", "File does not exist: ${file.absolutePath}")
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("UpdateChecker", "Install Error", e)
            Toast.makeText(context, "Eroare la instalare: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}

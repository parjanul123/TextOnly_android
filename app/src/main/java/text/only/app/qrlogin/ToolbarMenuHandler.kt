package text.only.app.qrlogin

import android.content.Intent
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import text.only.app.OnlyGuardActivity
import text.only.app.ProfileActivity
import text.only.app.R

object ToolbarMenuHandler {
    fun setupToolbar(activity: AppCompatActivity, toolbar: Toolbar?, btnSettings: ImageButton) {
        btnSettings.setOnClickListener {
            val popup = PopupMenu(activity, btnSettings)
            popup.menuInflater.inflate(R.menu.menu_toolbar, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_profile -> {
                        activity.startActivity(Intent(activity, ProfileActivity::class.java))
                        true
                    }
                    R.id.action_only_guard -> {
                        activity.startActivity(Intent(activity, OnlyGuardActivity::class.java))
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }
}
